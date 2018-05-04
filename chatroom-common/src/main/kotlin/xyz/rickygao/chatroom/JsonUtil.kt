package xyz.rickygao.chatroom

import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject


fun BufferedSource.readJson() = JSONObject(readByteString(readLong()).utf8())

fun BufferedSink.writeJson(jsonObject: JSONObject) {
    val json = jsonObject.toString().let(ByteString::encodeUtf8)
    writeLong(json.size().toLong())
    write(json)
    flush()
}

fun JSONObject.putTimestamp(): JSONObject =
        put("timestamp", System.currentTimeMillis())

fun JSONObject.getStringOrNull(key: String) = try {
    getString(key)
} catch (e: JSONException) {
    null
}