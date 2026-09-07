package com.fenyuan.liquor.modules.kingdee.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeCredential;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeCredentialService;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeLoginService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "金蝶账号密码")
@RestController
@RequestMapping("/api/kingdee/credentials")
@RequiredArgsConstructor
public class KingdeeCredentialController {

    private final KingdeeCredentialService service;
    private final KingdeeLoginService loginService;

    @ApiOperation("分页查询")
    @RequiresPermission("kingdee:credential:list")
    @GetMapping
    public Result<PageResult<KingdeeCredential>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String name) {
        return Result.ok(service.page(current, size, name));
    }

    @ApiOperation("启用账号列表")
    @RequiresPermission("kingdee:credential:list")
    @GetMapping("/enabled")
    public Result<List<KingdeeCredential>> listEnabled() {
        return Result.ok(service.listEnabled());
    }

    @ApiOperation("详情")
    @RequiresPermission("kingdee:credential:list")
    @GetMapping("/{id}")
    public Result<KingdeeCredential> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("kingdee:credential:add")
    @Log(value = "新增金蝶账号", module = "金蝶账号密码")
    @PostMapping
    public Result<Void> create(@RequestBody KingdeeCredential entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("kingdee:credential:edit")
    @Log(value = "更新金蝶账号", module = "金蝶账号密码")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody KingdeeCredential entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("kingdee:credential:delete")
    @Log(value = "删除金蝶账号", module = "金蝶账号密码")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }

    @ApiOperation("切换状态")
    @RequiresPermission("kingdee:credential:edit")
    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.ok();
    }

    @ApiOperation("测试登录")
    @RequiresPermission("kingdee:credential:edit")
    @PostMapping("/{id}/test-login")
    public Result<Map<String, Object>> testLogin(@PathVariable Long id,
                                                 @RequestParam(required = false) String dbId) {
        KingdeeCredential credential = service.getByIdWithPassword(id);
        String testDbId = dbId != null && !dbId.isEmpty() ? dbId : "61cbb7b2d5e132";
        try {
            String sessionId = loginService.login(
                    credential.getKingdeeUrl(), testDbId, credential.getUsername(), credential.getPassword());
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("sessionIdPreview", sessionId != null && sessionId.length() > 8
                    ? sessionId.substring(0, 8) + "..." : sessionId);
            data.put("message", "登录成功");
            return Result.ok(data);
        } catch (Exception e) {
            return Result.fail("登录失败：" + e.getMessage());
        }
    }
}
