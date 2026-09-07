package com.fenyuan.liquor.modules.kingdee.service;

import com.fenyuan.liquor.common.exception.BusinessException;
import com.fenyuan.liquor.modules.kingdee.dto.KingdeeAccountContext;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeAccountSet;
import com.fenyuan.liquor.modules.kingdee.entity.KingdeeCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KingdeeAccountResolverService {

    private final KingdeeAccountSetService accountSetService;
    private final KingdeeCredentialService credentialService;

    public KingdeeAccountContext resolveForLogin(Long accountSetId) {
        if (accountSetId == null) {
            throw new BusinessException("请选择金蝶账套");
        }
        KingdeeAccountSet accountSet = accountSetService.getById(accountSetId);
        if (accountSet.getStatus() == null || accountSet.getStatus() != 1) {
            throw new BusinessException("金蝶账套已禁用");
        }
        KingdeeCredential credential = credentialService.getByIdWithPassword(accountSet.getCredentialId());
        if (credential.getStatus() == null || credential.getStatus() != 1) {
            throw new BusinessException("金蝶账号已禁用");
        }
        return toContext(accountSet, credential);
    }

    public KingdeeAccountContext resolveForOperation(Long accountSetId) {
        if (accountSetId != null) {
            return resolveForLogin(accountSetId);
        }
        KingdeeAccountSet def = accountSetService.getDefault();
        if (def == null) {
            throw new BusinessException("未配置默认金蝶账套，请先在账套管理中设置");
        }
        KingdeeCredential credential = credentialService.getByIdWithPassword(def.getCredentialId());
        return toContext(def, credential);
    }

    private KingdeeAccountContext toContext(KingdeeAccountSet accountSet, KingdeeCredential credential) {
        KingdeeAccountContext ctx = new KingdeeAccountContext();
        ctx.setId(accountSet.getId());
        ctx.setAccountName(accountSet.getAccountName());
        ctx.setKingdeeUrl(credential.getKingdeeUrl());
        ctx.setDbId(accountSet.getDbId());
        ctx.setUsername(credential.getUsername());
        ctx.setPassword(credential.getPassword());
        ctx.setOrgCompanyCode(accountSet.getOrgCompanyCode());
        ctx.setUseOrgCode(accountSet.getUseOrgCode());
        ctx.setDefaultFormId(accountSet.getDefaultFormId());
        ctx.setDefaultAccountingPeriod(accountSet.getDefaultAccountingPeriod());
        ctx.setIsDefault(accountSet.getIsDefault());
        ctx.setStatus(accountSet.getStatus());
        return ctx;
    }
}
