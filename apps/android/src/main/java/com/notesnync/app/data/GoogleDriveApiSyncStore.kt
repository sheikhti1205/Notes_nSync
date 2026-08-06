package com.notesnync.app.data

import android.content.Context
import com.notesnync.app.domain.BackupCodec
import com.notesnync.app.domain.BackupSnapshot
import com.notesnync.app.domain.ParsedSyncConflictReport
import com.notesnync.app.domain.SyncConflictReport
import com.notesnync.app.domain.SyncProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationService
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GoogleDriveApiSyncStore(
    private val context: Context,
    private val authStore: GoogleDriveAuthStore,
) {
    suspend fun createChain(
        secret: CharArray,
        username: String,
        publicName: String,
        deviceName: String,
        snapshot: BackupSnapshot,
    ): SyncWriteResult = withContext(Dispatchers.IO) {
        val chainId = UUID.randomUUID().toString()
        val snapshotHash = snapshot.stableHash()
        val document = syncDocument(
            chainId = chainId,
            username = username,
            publicName = publicName,
            deviceName = deviceName,
            snapshotHash = snapshotHash,
            snapshotPayload = BackupCodec.encrypt(snapshot, secret),
            status = "Google Drive API sync chain created",
        )
        uploadOrCreateJson(accessToken(), SyncFileName, document)
        SyncWriteResult(chainId, snapshotHash, "Google Drive API sync chain created")
    }

    suspend fun restoreChain(secret: CharArray): SyncRestoreResult = withContext(Dispatchers.IO) {
        val accessToken = accessToken()
        val remote = readRemoteSync(accessToken) ?: error("No Notes'nync sync file found in Google Drive app data.")
        val snapshot = BackupCodec.decrypt(remote.snapshotPayload, secret)
        val hash = snapshot.stableHash()
        SyncRestoreResult(
            snapshot = snapshot,
            writeResult = SyncWriteResult(remote.chainId, hash, "Restored from Google Drive app data"),
        )
    }

    suspend fun syncNow(
        chainId: String,
        secret: CharArray,
        username: String,
        publicName: String,
        deviceName: String,
        lastSyncHash: String?,
        localSnapshot: BackupSnapshot,
    ): Pair<BackupSnapshot?, SyncWriteResult> = withContext(Dispatchers.IO) {
        val accessToken = accessToken()
        val localHash = localSnapshot.stableHash()
        val remote = readRemoteSync(accessToken)
        if (remote == null) {
            uploadOrCreateJson(
                accessToken,
                SyncFileName,
                syncDocument(chainId, username, publicName, deviceName, localHash, BackupCodec.encrypt(localSnapshot, secret), "Remote snapshot created"),
            )
            return@withContext null to SyncWriteResult(chainId, localHash, "Remote snapshot created")
        }

        val remoteSnapshot = BackupCodec.decrypt(remote.snapshotPayload, secret)
        val remoteHash = remoteSnapshot.stableHash()
        when {
            remoteHash == localHash -> {
                uploadOrCreateJson(accessToken, SyncFileName, syncDocument(chainId, username, publicName, deviceName, localHash, remote.snapshotPayload, "Already in sync"))
                null to SyncWriteResult(chainId, localHash, "Already in sync")
            }
            lastSyncHash == null || lastSyncHash == remoteHash -> {
                uploadOrCreateJson(accessToken, SyncFileName, syncDocument(chainId, username, publicName, deviceName, localHash, BackupCodec.encrypt(localSnapshot, secret), "Uploaded local changes"))
                null to SyncWriteResult(chainId, localHash, "Uploaded local changes")
            }
            lastSyncHash == localHash -> {
                remoteSnapshot to SyncWriteResult(chainId, remoteHash, "Downloaded remote changes")
            }
            else -> {
                uploadOrCreateJson(
                    accessToken,
                    ConflictFileName,
                    SyncConflictReport.build(
                        format = "notesnync-google-drive-conflict-v2",
                        localHash = localHash,
                        remoteHash = remoteHash,
                        deviceName = deviceName,
                        localSnapshot = localSnapshot,
                        remoteSnapshot = remoteSnapshot,
                    ),
                )
                null to SyncWriteResult(chainId, lastSyncHash, "Conflict detected; review before syncing", conflictCount = 1)
            }
        }
    }

    suspend fun readConflictReport(): ParsedSyncConflictReport? = withContext(Dispatchers.IO) {
        val accessToken = accessToken()
        val file = findFile(accessToken, ConflictFileName) ?: return@withContext null
        SyncConflictReport.parseOrNull(request(accessToken, "GET", "$DriveApi/files/${file.id}?alt=media"))
    }

    private suspend fun accessToken(): String = suspendCancellableCoroutine { continuation ->
        val authState = authStore.load()
        if (authState == null) {
            continuation.resumeWithException(IllegalStateException("Connect Google Drive first"))
            return@suspendCancellableCoroutine
        }
        val service = AuthorizationService(context)
        authState.performActionWithFreshTokens(service) { accessToken, _, exception ->
            service.dispose()
            if (exception != null) {
                continuation.resumeWithException(exception)
            } else if (accessToken.isNullOrBlank()) {
                continuation.resumeWithException(IllegalStateException("Google Drive access token is empty"))
            } else {
                authStore.save(authState)
                continuation.resume(accessToken)
            }
        }
    }

    private fun readRemoteSync(accessToken: String): RemoteSync? {
        val file = findFile(accessToken, SyncFileName) ?: return null
        val body = request(accessToken, "GET", "$DriveApi/files/${file.id}?alt=media")
        val json = JSONObject(body)
        return RemoteSync(
            fileId = file.id,
            chainId = json.optString("chainId").ifBlank { UUID.randomUUID().toString() },
            snapshotHash = json.optString("snapshotHash"),
            snapshotPayload = json.getString("snapshotPayload"),
        )
    }

    private fun uploadOrCreateJson(accessToken: String, fileName: String, body: String): String {
        val existing = findFile(accessToken, fileName)
        val method = if (existing == null) "POST" else "PATCH"
        val target = if (existing == null) "$DriveUpload/files?uploadType=multipart&fields=id" else "$DriveUpload/files/${existing.id}?uploadType=multipart&fields=id"
        val metadata = JSONObject()
            .put("name", fileName)
            .apply { if (existing == null) put("parents", org.json.JSONArray().put("appDataFolder")) }
            .toString()
        val boundary = "notesnync-${UUID.randomUUID()}"
        val payload = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(body)
            append("\r\n--$boundary--\r\n")
        }.toByteArray(Charsets.UTF_8)
        val response = requestBytes(accessToken, method, target, payload, "multipart/related; boundary=$boundary")
        return JSONObject(response).getString("id")
    }

    private fun findFile(accessToken: String, fileName: String): DriveFile? {
        val query = URLEncoder.encode("name = '$fileName' and trashed = false", "UTF-8")
        val url = "$DriveApi/files?spaces=appDataFolder&q=$query&fields=files(id,name,modifiedTime,size)&pageSize=1"
        val files = JSONObject(request(accessToken, "GET", url)).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        val row = files.getJSONObject(0)
        return DriveFile(row.getString("id"), row.getString("name"))
    }

    private fun request(accessToken: String, method: String, url: String): String =
        requestBytes(accessToken, method, url, null, null)

    private fun requestBytes(accessToken: String, method: String, url: String, body: ByteArray?, contentType: String?): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 20_000
            readTimeout = 30_000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType ?: "application/json; charset=UTF-8")
                setRequestProperty("Content-Length", body.size.toString())
            }
        }
        body?.let { connection.outputStream.use { out -> out.write(it) } }
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (responseCode !in 200..299) throw IOException("Google Drive API $method failed: HTTP $responseCode $response")
        return response
    }

    private fun syncDocument(
        chainId: String,
        username: String,
        publicName: String,
        deviceName: String,
        snapshotHash: String,
        snapshotPayload: String,
        status: String,
    ): String = JSONObject()
        .put("app", "Notes'nync")
        .put("format", "notesnync-google-drive-sync-v1")
        .put("chainId", chainId)
        .put("provider", SyncProvider.GoogleDrive.name)
        .put("providerLabel", "Google Drive API appDataFolder")
        .put("username", username)
        .put("publicName", publicName)
        .put("lastWriterDevice", deviceName)
        .put("snapshotHash", snapshotHash)
        .put("snapshotPayload", snapshotPayload)
        .put("status", status)
        .put("updatedAt", System.currentTimeMillis())
        .toString(2)

    private fun BackupSnapshot.stableHash(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(BackupCodec.encode(this).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private data class DriveFile(val id: String, val name: String)
    private data class RemoteSync(val fileId: String, val chainId: String, val snapshotHash: String, val snapshotPayload: String)

    companion object {
        const val AppDataFolderUri = "google-drive://appDataFolder/notesnync-sync.json"
        private const val SyncFileName = "notesnync-sync.json"
        private const val ConflictFileName = "notesnync-conflict.json"
        private const val DriveApi = "https://www.googleapis.com/drive/v3"
        private const val DriveUpload = "https://www.googleapis.com/upload/drive/v3"
    }
}
