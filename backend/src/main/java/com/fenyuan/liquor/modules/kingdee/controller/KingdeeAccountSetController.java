package com.fenyuan.liquor.modules.kingdee.controller;

import com.fenyuan.liquor.common.annotation.Log;
import com.fenyuan.liquor.common.annotation.RequiresPermission;
import com.fenyuan.liquor.common.result.PageResult;
import com.fenyuan.liquor.common.result.Result;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeAccountSet;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeCredential;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeAccountSetService;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeCredentialService;
import com.fenyuan.liquor.modules.kingdee.service.KingdeeLoginService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "金蝶账套")
@RestController
@RequestMapping("/api/kingdee/account-sets")
@RequiredArgsConstructor
public class KingdeeAccountSetController {

    private final KingdeeAccountSetService service;
    private final KingdeeCredentialService credentialService;
    private final KingdeeLoginService loginService;

    @ApiOperation("分页查询")
    @RequiresPermission("kingdee:accountSet:list")
    @GetMapping
    public Result<PageResult<KingdeeAccountSet>> page(@RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      @RequestParam(required = false) String accountName) {
        return Result.ok(service.page(current, size, accountName));
    }

    @ApiOperation("启用账套列表")
    @RequiresPermission("kingdee:accountSet:list")
    @GetMapping("/enabled")
    public Result<List<KingdeeAccountSet>> listEnabled() {
        return Result.ok(service.listEnabled());
    }

    @ApiOperation("默认账套")
    @RequiresPermission("kingdee:accountSet:list")
    @GetMapping("/default")
    public Result<KingdeeAccountSet> getDefault() {
        KingdeeAccountSet accountSet = service.getDefault();
        if (accountSet == null) {
            return Result.fail(404, "未设置默认账套");
        }
        return Result.ok(accountSet);
    }

    @ApiOperation("详情")
    @RequiresPermission("kingdee:accountSet:list")
    @GetMapping("/{id}")
    public Result<KingdeeAccountSet> get(@PathVariable Long id) {
        return Result.ok(service.getById(id));
    }

    @ApiOperation("新增")
    @RequiresPermission("kingdee:accountSet:add")
    @Log(value = "新增金蝶账套", module = "金蝶账套")
    @PostMapping
    public Result<Void> create(@RequestBody KingdeeAccountSet entity) {
        service.create(entity);
        return Result.ok();
    }

    @ApiOperation("更新")
    @RequiresPermission("kingdee:accountSet:edit")
    @Log(value = "更新金蝶账套", module = "金蝶账套")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody KingdeeAccountSet entity) {
        service.update(id, entity);
        return Result.ok();
    }

    @ApiOperation("删除")
    @RequiresPermission("kingdee:accountSet:delete")
    @Log(value = "删除金蝶账套", module = "金蝶账套")
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable String ids) {
        service.deleteByIds(ids);
        return Result.ok();
    }

    @ApiOperation("设为默认")
    @RequiresPermission("kingdee:accountSet:edit")
    @PostMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        service.setDefault(id);
        return Result.ok();
    }

    @ApiOperation("切换状态")
    @RequiresPermission("kingdee:accountSet:edit")
    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        service.toggleStatus(id);
        return Result.ok();
    }

    @ApiOperation("测试账套登录")
    @RequiresPermission("kingdee:accountSet:edit")
    @PostMapping("/{id}/test-login")
    public Result<Map<String, Object>> testLogin(@PathVariable Long id) {
        KingdeeAccountSet accountSet = service.getById(id);
        KingdeeCredential credential = credentialService.getByIdWithPassword(accountSet.getCredentialId());
        try {
            String sessionId = loginService.login(
                    credential.getKingdeeUrl(),
                    accountSet.getDbId(),
                    credential.getUsername(),
                    credential.getPassword());
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("sessionIdPreview", sessionId != null && sessionId.length() > 8
                    ? sessionId.substring(0, 8) + "..." : sessionId);
            data.put("message", "账套登录成功");
            return Result.ok(data);
        } catch (Exception e) {
            return Result.fail("账套登录失败：" + e.getMessage());
        }
    }
}
