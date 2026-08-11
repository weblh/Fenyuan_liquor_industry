package com.fenyuan.liquor.modules.monitor.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Long userId;
    private String module;
    private String operation;
    private String method;
    private String params;
    private String result;
    private String ip;
    private String location;
    private String browser;
    private String os;
    private Integer status;
    private String errorMsg;
    private Long time;
    private LocalDateTime createTime;
}
