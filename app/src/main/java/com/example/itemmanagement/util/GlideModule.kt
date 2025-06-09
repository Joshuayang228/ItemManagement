package com.example.itemmanagement.util

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.ExternalPreferredCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import java.io.InputStream

@GlideModule
class ItemManagementGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 设置内存缓存大小 (20MB)
        val memoryCacheSizeBytes = 1024 * 1024 * 20
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))

        // 设置磁盘缓存大小 (100MB)
        val diskCacheSizeBytes = 1024 * 1024 * 100
        builder.setDiskCache(ExternalPreferredCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))

        // 设置默认请求选项
        builder.setDefaultRequestOptions(
            RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .placeholder(com.example.itemmanagement.R.drawable.ic_image_placeholder)
                .error(com.example.itemmanagement.R.drawable.ic_image_error)
        )
    }

    // 禁用解析清单文件
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}