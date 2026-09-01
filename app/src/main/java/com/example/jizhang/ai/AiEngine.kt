package com.example.jizhang.ai

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import kotlinx.coroutines.runBlocking

/**
 * 本地 LLM 引擎桥接（封装 llama-android 的 suspend API，供 Java 后台线程调用）
 */
object AiEngine {

    private var model: LlamaModel? = null

    @JvmStatic
    fun isLoaded(): Boolean = model != null

    /** 加载 GGUF 模型（阻塞，需在后台线程调用） */
    @JvmStatic
    fun loadModel(modelPath: String, contextSize: Int, threads: Int): Boolean {
        return runBlocking {
            try {
                model?.let { Llama.releaseModel(it) }
                model = Llama.loadModel(
                    modelPath = modelPath,
                    config = LlamaConfig(contextSize = contextSize, threads = threads),
                )
                true
            } catch (e: Throwable) {
                model = null
                false
            }
        }
    }

    /** 文本补全（阻塞，需在后台线程调用） */
    @JvmStatic
    fun complete(prompt: String, systemPrompt: String, maxTokens: Int): String? {
        val m = model ?: return null
        return runBlocking {
            try {
                val result = Llama.complete(
                    model = m,
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    maxTokens = maxTokens,
                )
                result.text
            } catch (e: Throwable) {
                null
            }
        }
    }

    @JvmStatic
    fun release() {
        val m = model ?: return
        model = null
        runBlocking {
            try {
                Llama.releaseModel(m)
            } catch (e: Throwable) {
            }
        }
    }
}
