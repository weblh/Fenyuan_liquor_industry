package com.fenyuan.liquor.modules.kingdee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kingdee_credential")
public class KingdeeCredential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String kingdeeUrl;

    private String username;

    private String password;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
