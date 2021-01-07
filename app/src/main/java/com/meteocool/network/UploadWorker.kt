package com.meteocool.network

import android.content.Context
import androidx.work.*
import com.google.gson.Gson
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

/**
 * Upload post requests.
 */
class UploadWorker(private val context: Context, params: WorkerParameters) :
    Worker(context, params) {

    /**
     * Input data is converted to a simple json.
     * IMPORTANT: Always needs to have a Pair("url", <Endpoint-URL as String> in input data to perform a successfull upload.
     */
    override fun doWork(): Result {
        val url = URL(inputData.getString("url"))
        val dataForUpload = mutableMapOf<String, Any>()
        dataForUpload.putAll(inputData.keyValueMap)
        dataForUpload.remove("url")
        val gsonBuilder = Gson().newBuilder().create()
        val jsonAsString = gsonBuilder.toJson(dataForUpload)
        Timber.d("JSON $jsonAsString")
        return postJSON(url, jsonAsString)
    }

    private fun postJSON(url: URL, jsonAsString: String): Result {
        try {
            with(url.openConnection() as HttpURLConnection) {
                // optional default is GET
                requestMethod = "POST"
                setRequestProperty("charset", "utf-8")
                setRequestProperty("Content-lenght", jsonAsString)
                setRequestProperty("Content-Type", "application/json")

                val wr = OutputStreamWriter(outputStream)

                wr.write(jsonAsString)
                wr.flush()

                Timber.d("URL $url")
                Timber.d("HTTP-Response $responseCode")

                BufferedReader(InputStreamReader(inputStream)).use {
                    val response = StringBuffer()

                    var inputLine = it.readLine()
                    while (inputLine != null) {
                        response.append(inputLine)
                        inputLine = it.readLine()
                    }
                    it.close()
                    Timber.d("$response")
                    response.toString()
                }
                if (responseCode in 200..399) {
                    return Result.success()
                }
                return Result.failure()
            }
        } catch (e: Exception) {
            Timber.e(e)
            return Result.failure()
        }
    }

    companion object {
        fun createInputData(data: Map<String, Any>): Data {
            return Data.Builder()
                .putAll(data)
                .build()
        }
    }
}


