package com.premium.member.c.biz.ai.controller;

import com.premium.member.c.biz.ai.service.AssetExposeService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅用于 AI 供应商临时读取生成任务的输入素材。
 */
@RestController
@RequestMapping("/c/assets")
public class CAssetController {

    private final AssetExposeService exposeService;

    public CAssetController(AssetExposeService exposeService) {
        this.exposeService = exposeService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<org.springframework.core.io.Resource> file(@PathVariable String token) {
        AssetExposeService.PublicAsset asset = exposeService.open(token);
        MediaType mediaType = MediaType.parseMediaType(asset.mime());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noStore())
                .body(asset.resource());
    }
}
