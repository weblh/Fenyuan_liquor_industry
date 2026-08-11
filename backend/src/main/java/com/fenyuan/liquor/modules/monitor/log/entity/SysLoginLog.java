package com.fenyuan.liquor.modules.monitor.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Long userId;
    private String ip;
    private String location;
    private String browser;
    private String os;
    private Integer status;
    private String msg;
    private LocalDateTime loginTime;
}
