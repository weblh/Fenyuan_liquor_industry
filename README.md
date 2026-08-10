# 汾源酒业经营体
# Leasing_entity

## 快速开始

项目骨架：`backend/`（Spring Boot 2.7 + Java 8）与 `frontend/`（React + Vite + Ant Design）。

### 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 端口：`http://localhost:4001`
- 开发默认使用内存 H2，接口文档：`http://localhost:4001/doc.html`
- 账号：`admin` / `admin123`
- 切换 MySQL：`--spring.profiles.active=prod`

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 访问：`http://localhost:4001`
- `/api` 已代理到后端 `4001`

### 目录结构

```
Leasing_entity/
├── backend/          # Java Spring Boot 后端
├── frontend/         # React 管理端
├── tools/            # 本地 Maven（已 gitignore）
└── README.md
```

---
通用型后台管理系统开发提示词

## 项目概述
开发一个基于 **React + Ant Design** 前端和 **Java Spring Boot** 后端的通用型后台管理系统，包含完整的权限控制、用户管理、系统设置等核心功能模块。

---

## 技术栈规范

### 前端技术栈
- **框架**: React 18.x
- **UI组件库**: Ant Design 5.x
- **状态管理**: Redux Toolkit / Zustand
- **路由**: React Router v6
- **HTTP请求**: Axios
- **构建工具**: Vite / Create React App
- **CSS方案**: CSS Modules / styled-components
- **国际化**: react-i18next（可选）

### 后端技术栈
- **JDK版本**: Java 1.8
- **框架**: Spring Boot 2.7.x
- **安全框架**: Spring Security + JWT
- **ORM**: MyBatis-Plus 3.5.x
- **数据库**: MySQL 8.0 / PostgreSQL
- **缓存**: Redis（可选）
- **API文档**: Swagger 2 / Knife4j
- **工具库**: Hutool / Guava

---

## 功能模块要求

### 1. 登录/认证模块
#### 功能点：
- [ ] 用户名+密码登录
- [ ] 验证码校验（图形验证码）
- [ ] JWT Token认证
- [ ] 登录失败次数限制（可选）
- [ ] 记住我功能
- [ ] 退出登录

#### 前端页面：
- 登录页面（居中布局，背景可配置）
- 登录表单（用户名、密码、验证码）
- 登录状态持久化（localStorage）

#### 后端接口：
POST /api/auth/login - 用户登录
POST /api/auth/logout - 退出登录
GET /api/auth/captcha - 获取验证码
POST /api/auth/refresh - 刷新Token
GET /api/auth/info - 获取当前用户信息

text

### 2. 系统管理模块

#### 2.1 用户管理
- [ ] 用户列表（分页、搜索、筛选）
- [ ] 新增用户
- [ ] 编辑用户
- [ ] 删除用户（批量删除）
- [ ] 重置密码
- [ ] 用户状态切换（启用/禁用）
- [ ] 用户导入/导出（Excel）

#### 2.2 角色管理
- [ ] 角色列表（CRUD）
- [ ] 角色权限分配（菜单权限+按钮权限）
- [ ] 角色状态管理
- [ ] 角色数据权限（可选）

#### 2.3 菜单管理
- [ ] 菜单树形列表
- [ ] 新增/编辑/删除菜单
- [ ] 菜单类型（目录/菜单/按钮）
- [ ] 菜单图标设置
- [ ] 菜单排序
- [ ] 前端路由配置同步

#### 2.4 部门管理（可选）
- [ ] 部门树形结构
- [ ] 部门CRUD
- [ ] 用户归属部门

### 3. 系统设置模块

#### 3.1 系统配置
- [ ] 系统名称/Logo配置
- [ ] 系统主题色设置
- [ ] 首页风格配置

#### 3.2 操作日志
- [ ] 登录日志记录
- [ ] 操作日志记录（AOP实现）
- [ ] 日志查询（按时间、用户、操作类型）
- [ ] 日志导出

#### 3.3 系统监控（可选）
- [ ] 服务器信息（CPU、内存、磁盘）
- [ ] 在线用户管理
- [ ] Redis监控（可选）

### 4. 权限控制

#### 4.1 后端权限
- [ ] 基于URL的权限拦截
- [ ] 基于方法的权限注解（@PreAuthorize）
- [ ] 动态权限加载
- [ ] 数据权限隔离（可选）

#### 4.2 前端权限
- [ ] 路由权限控制
- [ ] 菜单权限控制
- [ ] 按钮权限控制（v-permission指令）
- [ ] 页面级权限

### 5. 布局与导航模块（前端）

> 位于顶栏 Header 与主内容区之间，提供页面定位与多路由切换能力。

#### 5.1 面包屑目录（Breadcrumb）
- [x] 根据当前路由与菜单树自动生成层级目录（首页 / 目录 / 菜单）
- [x] 支持点击可跳转的层级节点
- [x] 目录类型（type=0）节点仅展示、不跳转到无效路由

#### 5.2 路由多页签导航条（TagsView / Tabs）
- [x] 访问页面后自动新增页签，展示菜单名称
- [x] 点击页签切换路由
- [x] 关闭页签（首页固定不可关闭）
- [x] 右键菜单：刷新、关闭当前、关闭其他、关闭左侧、关闭右侧、关闭全部
- [x] 退出登录时清空已访问页签

#### 5.3 布局结构约定
```
┌──────── Sidebar ────────┬────────── Header（折叠按钮 / 系统标题 / 用户）──────────┐
│                         ├────────── TagsView（路由多页签导航条）──────────────────┤
│                         ├────────── Breadcrumb（目录面包屑）──────────────────────┤
│                         ├────────── Content（业务页面）───────────────────────────┤
│                         └────────── Footer────────────────────────────────────────┘
```

---

## 项目结构规范

### 前端项目结构
src/
├── api/ # API接口层
│ ├── modules/ # 按模块划分
│ │ ├── auth.js
│ │ ├── user.js
│ │ ├── role.js
│ │ └── menu.js
│ └── index.js # axios配置
├── assets/ # 静态资源
│ ├── images/
│ └── styles/
├── components/ # 公共组件
│ ├── Layout/ # 布局组件
│ │ ├── Header/
│ │ ├── Sidebar/
│ │ ├── TagsView/      # 路由多页签导航条
│ │ ├── BreadcrumbNav/ # 面包屑目录
│ │ └── Footer/
│ ├── Common/ # 通用组件
│ │ ├── SearchForm/
│ │ └── Table/
│ └── Permission/ # 权限组件
├── hooks/ # 自定义Hooks
├── pages/ # 页面组件
│ ├── Login/
│ ├── Dashboard/
│ ├── System/
│ │ ├── User/
│ │ ├── Role/
│ │ └── Menu/
│ └── Settings/
├── router/ # 路由配置
│ ├── index.js # 路由定义
│ └── permission.js # 路由权限
├── store/ # 状态管理
│ ├── modules/
│ │ ├── user.js
│ │ ├── app.js
│ │ ├── permission.js
│ │ └── tagsView.js   # 已访问页签状态
│ └── index.js
├── utils/ # 工具函数
│ ├── request.js # 请求封装
│ ├── storage.js # 存储工具
│ ├── validator.js # 表单验证
│ ├── menu.js        # 菜单路径/面包屑解析
│ └── permission.js # 权限工具
├── App.js
└── index.js

text

### 后端项目结构（Spring Boot）
src/main/java/com/example/admin/
├── AdminApplication.java # 启动类
├── common/ # 公共模块
│ ├── annotation/ # 自定义注解
│ │ ├── RequiresPermission.java
│ │ └── Log.java
│ ├── aspect/ # AOP切面
│ │ ├── LogAspect.java
│ │ └── PermissionAspect.java
│ ├── config/ # 配置类
│ │ ├── WebConfig.java
│ │ ├── SecurityConfig.java
│ │ ├── MybatisPlusConfig.java
│ │ └── SwaggerConfig.java
│ ├── exception/ # 异常处理
│ │ ├── BusinessException.java
│ │ └── GlobalExceptionHandler.java
│ ├── filter/ # 过滤器
│ │ └── JwtAuthenticationFilter.java
│ ├── handler/ # 处理器
│ │ └── MyMetaObjectHandler.java
│ └── utils/ # 工具类
│ ├── JwtUtils.java
│ ├── RedisUtils.java
│ └── SecurityUtils.java
├── modules/ # 业务模块
│ ├── auth/ # 认证模块
│ │ ├── controller/
│ │ ├── service/
│ │ ├── mapper/
│ │ └── entity/
│ ├── system/ # 系统管理
│ │ ├── user/
│ │ ├── role/
│ │ ├── menu/
│ │ └── dept/
│ ├── monitor/ # 系统监控
│ └── settings/ # 系统设置
├── security/ # 安全模块
│ ├── UserDetailsServiceImpl.java
│ └── JwtTokenUtil.java
├── generator/ # 代码生成器（可选）
└── resources/
├── application.yml
└── mapper/ # MyBatis XML文件

text

---

## 数据库设计规范

### 核心表结构

#### 用户表 (sys_user)
```sql
CREATE TABLE sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(50) NOT NULL COMMENT '用户名',
    password     VARCHAR(100) NOT NULL COMMENT '密码',
    nickname     VARCHAR(50) COMMENT '昵称',
    email        VARCHAR(100) COMMENT '邮箱',
    phone        VARCHAR(20) COMMENT '手机号',
    avatar       VARCHAR(255) COMMENT '头像',
    dept_id      BIGINT COMMENT '部门ID',
    status       TINYINT DEFAULT 1 COMMENT '状态(0:禁用,1:启用)',
    is_admin     TINYINT DEFAULT 0 COMMENT '是否管理员',
    login_ip     VARCHAR(50) COMMENT '最后登录IP',
    login_date   DATETIME COMMENT '最后登录时间',
    create_by    BIGINT COMMENT '创建人',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT COMMENT '更新人',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag     TINYINT DEFAULT 0 COMMENT '删除标志',
    UNIQUE KEY uk_username (username)
) COMMENT='用户表';
角色表 (sys_role)
sql
CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    name        VARCHAR(50) NOT NULL COMMENT '角色名称',
    code        VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    sort        INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志',
    UNIQUE KEY uk_code (code)
) COMMENT='角色表';
菜单表 (sys_menu)
sql
CREATE TABLE sys_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id   BIGINT DEFAULT 0 COMMENT '父菜单ID',
    name        VARCHAR(50) NOT NULL COMMENT '菜单名称',
    path        VARCHAR(200) COMMENT '路由路径',
    component   VARCHAR(200) COMMENT '组件路径',
    permission  VARCHAR(100) COMMENT '权限标识',
    type        TINYINT NOT NULL COMMENT '类型(0:目录,1:菜单,2:按钮)',
    icon        VARCHAR(50) COMMENT '图标',
    sort        INT DEFAULT 0 COMMENT '排序',
    visible     TINYINT DEFAULT 1 COMMENT '是否可见',
    status      TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志'
) COMMENT='菜单表';
用户角色关联表 (sys_user_role)
sql
CREATE TABLE sys_user_role (
    user_id   BIGINT NOT NULL COMMENT '用户ID',
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) COMMENT='用户角色关联表';
角色菜单关联表 (sys_role_menu)
sql
CREATE TABLE sys_role_menu (
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    menu_id   BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) COMMENT='角色菜单关联表';
操作日志表 (sys_log)
sql
CREATE TABLE sys_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    username    VARCHAR(50) COMMENT '用户名',
    operation   VARCHAR(200) COMMENT '操作描述',
    method      VARCHAR(200) COMMENT '方法名',
    params      TEXT COMMENT '请求参数',
    ip          VARCHAR(50) COMMENT '请求IP',
    location    VARCHAR(100) COMMENT '操作地点',
    status      TINYINT COMMENT '操作状态(0:失败,1:成功)',
    error_msg   TEXT COMMENT '错误信息',
    time        BIGINT COMMENT '执行耗时(ms)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='操作日志表';
API接口设计规范
统一响应格式
json
{
    "code": 200,
    "msg": "操作成功",
    "data": {},
    "timestamp": 1700000000000
}
分页响应格式
json
{
    "code": 200,
    "msg": "查询成功",
    "data": {
        "records": [],
        "total": 100,
        "size": 10,
        "current": 1,
        "pages": 10
    }
}
核心API接口列表
认证接口
text
POST /api/auth/login
请求体: { username, password, captcha, captchaKey }
响应: { token, userInfo }

POST /api/auth/logout
请求头: Authorization: Bearer {token}

GET /api/auth/captcha
响应: { captchaKey, captchaImage }
用户管理接口
text
GET    /api/users                - 分页查询用户列表
GET    /api/users/{id}           - 查询用户详情
POST   /api/users                - 新增用户
PUT    /api/users/{id}           - 更新用户
DELETE /api/users/{ids}          - 删除用户(批量)
PUT    /api/users/{id}/status    - 更新用户状态
PUT    /api/users/{id}/password  - 重置密码
GET    /api/users/export         - 导出用户
POST   /api/users/import         - 导入用户
角色管理接口
text
GET    /api/roles                - 查询角色列表
GET    /api/roles/{id}           - 查询角色详情
POST   /api/roles                - 新增角色
PUT    /api/roles/{id}           - 更新角色
DELETE /api/roles/{ids}          - 删除角色(批量)
PUT    /api/roles/{id}/status    - 更新角色状态
GET    /api/roles/{id}/permissions - 获取角色权限
PUT    /api/roles/{id}/permissions - 分配角色权限
菜单管理接口
text
GET    /api/menus                - 查询菜单列表(树形)
GET    /api/menus/{id}           - 查询菜单详情
POST   /api/menus                - 新增菜单
PUT    /api/menus/{id}           - 更新菜单
DELETE /api/menus/{ids}          - 删除菜单(批量)
GET    /api/menus/routes         - 获取路由配置
GET    /api/menus/user           - 获取用户菜单
前端核心代码提示
1. Axios配置示例
javascript
// utils/request.js
import axios from 'axios';
import { message } from 'antd';
import { getToken, removeToken } from './storage';

const request = axios.create({
    baseURL: process.env.REACT_APP_API_URL || '/api',
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' }
});

// 请求拦截器
request.interceptors.request.use(
    config => {
        const token = getToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器
request.interceptors.response.use(
    response => {
        const { code, msg, data } = response.data;
        if (code === 200) {
            return data;
        } else if (code === 401) {
            removeToken();
            window.location.href = '/login';
            return Promise.reject(new Error('未授权'));
        } else {
            message.error(msg || '请求失败');
            return Promise.reject(new Error(msg));
        }
    },
    error => {
        message.error(error.message || '网络错误');
        return Promise.reject(error);
    }
);

export default request;
2. 路由权限控制
javascript
// router/permission.js
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { getRoutes } from '@/api/menu';
import { setRoutes } from '@/store/modules/permission';

export const usePermissionRoutes = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { routes, isAuthenticated } = useSelector(state => state.permission);

    useEffect(() => {
        if (isAuthenticated && routes.length === 0) {
            fetchRoutes();
        }
    }, [isAuthenticated]);

    const fetchRoutes = async () => {
        try {
            const data = await getRoutes();
            dispatch(setRoutes(data));
        } catch (error) {
            console.error('获取路由失败:', error);
        }
    };

    return routes;
};
3. 权限指令
javascript
// directives/permission.js
import { usePermissions } from '@/hooks/usePermissions';

export const Permission = ({ children, permission }) => {
    const hasPermission = usePermissions(permission);
    return hasPermission ? children : null;
};

// 使用示例
<Permission permission="user:add">
    <Button type="primary">新增用户</Button>
</Permission>
后端核心代码提示
1. Spring Security配置
java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/auth/**", "/api/captcha").permitAll()
                .antMatchers("/swagger-ui/**", "/v2/api-docs").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
2. JWT工具类
java
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    public String generateToken(String username, Map<String, Object> claims) {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
    
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
3. 全局异常处理
java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return Result.error(400, message);
    }
    
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常:", e);
        return Result.error(500, "系统异常，请稍后重试");
    }
}
4. MyBatis-Plus分页配置
java
@Configuration
public class MybatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "delFlag", Integer.class, 0);
            }
            
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
开发规范
Git提交规范
text
feat: 新功能
fix: 修复bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
perf: 性能优化
test: 测试相关
chore: 构建/工具链更新
代码规范
前端使用ESLint + Prettier

后端使用Alibaba Java Coding Guidelines

所有API接口必须有Swagger注解

所有数据库操作必须使用参数化查询

敏感信息必须加密存储

部署说明
前端打包
bash
npm run build
# 生成dist目录，配置Nginx指向dist
后端打包
bash
mvn clean package
# 生成target/admin.jar
# 运行: java -jar admin.jar --spring.profiles.active=prod
Docker部署（可选）
dockerfile
# Dockerfile示例
FROM openjdk:8-jre-alpine
COPY target/admin.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
额外功能建议
代码生成器 - 基于MyBatis-Plus生成CRUD代码

数据字典 - 系统级数据字典管理

文件上传 - 统一文件上传组件

消息通知 - WebSocket实时通知

定时任务 - 基于Quartz的任务调度

接口限流 - 基于Redis的接口限流

操作审计 - 敏感操作审计日志

多环境配置 - dev/test/prod环境隔离

注意事项
所有密码必须使用BCrypt加密

敏感操作必须记录操作日志

SQL查询必须使用预编译防止注入

前后端数据传输必须使用HTTPS

Token必须设置合理的过期时间

接口必须进行权限校验

文件上传必须限制大小和类型

系统配置必须支持热更新

文档版本: v1.0
创建日期: 2026-07-27
适用项目: 通用后台管理系统

markdown
# Gongcheng Equipment Leasing 后台管理系统 - 数据库配置提示词

## 数据库基本信息
- **数据库名称**: gongcheng_leasing
- **数据库类型**: MySQL 8.0
- **连接地址**: localhost:3306
- **账号**: root
- **密码**: 123456
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_general_ci

---

## 一、数据库创建脚本

### 1. 创建数据库
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS gongcheng_leasing 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_general_ci;

-- 使用数据库
USE gongcheng_leasing;

-- 查看当前数据库
SELECT DATABASE();
2. 应用配置文件
application.yml 配置
yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/gongcheng_leasing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    # 连接池配置
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      auto-commit: true
      idle-timeout: 30000
      pool-name: GongchengHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1

  # MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: del_flag
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.gongcheng.leasing.modules.*.entity
application-dev.yml 配置（开发环境）
yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gongcheng_leasing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
二、完整表结构创建脚本
1. 用户表 (sys_user)
sql
CREATE TABLE sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(50) NOT NULL COMMENT '用户名',
    password     VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    nickname     VARCHAR(50) COMMENT '昵称',
    real_name    VARCHAR(50) COMMENT '真实姓名',
    email        VARCHAR(100) COMMENT '邮箱',
    phone        VARCHAR(20) COMMENT '手机号',
    avatar       VARCHAR(255) COMMENT '头像URL',
    sex          TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    dept_id      BIGINT COMMENT '部门ID',
    post_id      BIGINT COMMENT '岗位ID',
    status       TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_admin     TINYINT DEFAULT 0 COMMENT '是否管理员：0-否，1-是',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    last_login_time DATETIME COMMENT '最后登录时间',
    login_count  INT DEFAULT 0 COMMENT '登录次数',
    pwd_reset_time DATETIME COMMENT '密码重置时间',
    create_by    BIGINT COMMENT '创建人ID',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT COMMENT '更新人ID',
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag     TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark       VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
2. 角色表 (sys_role)
sql
CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    name        VARCHAR(50) NOT NULL COMMENT '角色名称',
    code        VARCHAR(50) NOT NULL COMMENT '角色编码（唯一标识）',
    description VARCHAR(255) COMMENT '角色描述',
    sort        INT DEFAULT 0 COMMENT '排序（数值越小越靠前）',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    data_scope  TINYINT DEFAULT 1 COMMENT '数据范围：1-全部数据，2-本部门及子部门，3-本部门，4-本人，5-自定义',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
3. 菜单表 (sys_menu)
sql
CREATE TABLE sys_menu (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    parent_id   BIGINT DEFAULT 0 COMMENT '父菜单ID（0表示顶级菜单）',
    name        VARCHAR(50) NOT NULL COMMENT '菜单名称',
    path        VARCHAR(200) COMMENT '路由路径',
    component   VARCHAR(200) COMMENT '组件路径',
    permission  VARCHAR(100) COMMENT '权限标识（如：user:add）',
    type        TINYINT NOT NULL COMMENT '菜单类型：0-目录，1-菜单，2-按钮',
    icon        VARCHAR(50) COMMENT '图标（Ant Design图标名称）',
    sort        INT DEFAULT 0 COMMENT '排序',
    visible     TINYINT DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    keep_alive  TINYINT DEFAULT 0 COMMENT '是否缓存：0-否，1-是',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    KEY idx_parent_id (parent_id),
    KEY idx_type (type),
    KEY idx_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';
4. 部门表 (sys_dept)
sql
CREATE TABLE sys_dept (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    parent_id   BIGINT DEFAULT 0 COMMENT '父部门ID（0表示顶级）',
    name        VARCHAR(50) NOT NULL COMMENT '部门名称',
    code        VARCHAR(50) COMMENT '部门编码',
    leader      VARCHAR(50) COMMENT '负责人',
    phone       VARCHAR(20) COMMENT '联系电话',
    email       VARCHAR(100) COMMENT '邮箱',
    sort        INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    KEY idx_parent_id (parent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
5. 岗位表 (sys_post)
sql
CREATE TABLE sys_post (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '岗位ID',
    name        VARCHAR(50) NOT NULL COMMENT '岗位名称',
    code        VARCHAR(50) NOT NULL COMMENT '岗位编码',
    sort        INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';
6. 用户角色关联表 (sys_user_role)
sql
CREATE TABLE sys_user_role (
    user_id   BIGINT NOT NULL COMMENT '用户ID',
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
7. 角色菜单关联表 (sys_role_menu)
sql
CREATE TABLE sys_role_menu (
    role_id   BIGINT NOT NULL COMMENT '角色ID',
    menu_id   BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_id (role_id),
    KEY idx_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';
8. 操作日志表 (sys_oper_log)
sql
CREATE TABLE sys_oper_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    username    VARCHAR(50) COMMENT '操作用户名',
    user_id     BIGINT COMMENT '操作用户ID',
    module      VARCHAR(50) COMMENT '操作模块',
    operation   VARCHAR(200) COMMENT '操作描述',
    method      VARCHAR(200) COMMENT '请求方法名',
    params      TEXT COMMENT '请求参数',
    result      TEXT COMMENT '返回结果',
    ip          VARCHAR(50) COMMENT '请求IP地址',
    location    VARCHAR(100) COMMENT '操作地点（根据IP解析）',
    browser     VARCHAR(50) COMMENT '浏览器',
    os          VARCHAR(50) COMMENT '操作系统',
    status      TINYINT COMMENT '操作状态：0-失败，1-成功',
    error_msg   TEXT COMMENT '错误信息',
    time        BIGINT COMMENT '执行耗时（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_user_id (user_id),
    KEY idx_create_time (create_time),
    KEY idx_status (status),
    KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
9. 登录日志表 (sys_login_log)
sql
CREATE TABLE sys_login_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    username    VARCHAR(50) COMMENT '用户名',
    user_id     BIGINT COMMENT '用户ID',
    ip          VARCHAR(50) COMMENT '登录IP',
    location    VARCHAR(100) COMMENT '登录地点',
    browser     VARCHAR(50) COMMENT '浏览器',
    os          VARCHAR(50) COMMENT '操作系统',
    status      TINYINT COMMENT '登录状态：0-失败，1-成功',
    msg         VARCHAR(255) COMMENT '提示信息',
    login_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_username (username),
    KEY idx_login_time (login_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
10. 系统配置表 (sys_config)
sql
CREATE TABLE sys_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    name        VARCHAR(50) NOT NULL COMMENT '配置名称',
    key         VARCHAR(50) NOT NULL COMMENT '配置键（唯一标识）',
    value       TEXT COMMENT '配置值',
    type        VARCHAR(20) DEFAULT 'string' COMMENT '配置类型：string, number, boolean, json',
    group_name  VARCHAR(50) COMMENT '配置分组',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_system   TINYINT DEFAULT 0 COMMENT '是否系统内置：0-否，1-是',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_key (key),
    KEY idx_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
11. 字典类型表 (sys_dict_type)
sql
CREATE TABLE sys_dict_type (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典类型ID',
    name        VARCHAR(50) NOT NULL COMMENT '字典名称',
    code        VARCHAR(50) NOT NULL COMMENT '字典编码（唯一标识）',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';
12. 字典数据表 (sys_dict_data)
sql
CREATE TABLE sys_dict_data (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '字典数据ID',
    dict_type_id BIGINT NOT NULL COMMENT '字典类型ID',
    label       VARCHAR(50) NOT NULL COMMENT '数据标签',
    value       VARCHAR(50) NOT NULL COMMENT '数据值',
    sort        INT DEFAULT 0 COMMENT '排序',
    status      TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    is_default  TINYINT DEFAULT 0 COMMENT '是否默认：0-否，1-是',
    css_class   VARCHAR(50) COMMENT 'CSS样式类',
    list_class  VARCHAR(50) COMMENT '列表样式类',
    create_by   BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT COMMENT '更新人ID',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag    TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
    remark      VARCHAR(255) COMMENT '备注',
    KEY idx_dict_type_id (dict_type_id),
    KEY idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';
三、初始化数据脚本
1. 插入默认管理员账号
sql
-- 密码：admin123，使用BCrypt加密
INSERT INTO sys_user (username, password, nickname, real_name, email, phone, is_admin, status) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', '系统管理员', 'admin@gongcheng.com', '13800138000', 1, 1);
2. 插入默认角色
sql
INSERT INTO sys_role (name, code, description, sort, status, data_scope) 
VALUES ('超级管理员', 'ROLE_ADMIN', '系统最高权限角色', 1, 1, 1);

INSERT INTO sys_role (name, code, description, sort, status, data_scope) 
VALUES ('普通用户', 'ROLE_USER', '普通用户角色', 2, 1, 3);
3. 插入默认菜单数据
sql
-- 顶级目录
INSERT INTO sys_menu (parent_id, name, path, component, type, icon, sort, visible, status) 
VALUES (0, '系统管理', '/system', 'Layout', 0, 'SettingOutlined', 1, 1, 1);

-- 系统管理子菜单
INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '用户管理', '/system/user', 'system/user/index', 'system:user:list', 1, 'UserOutlined', 1, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '角色管理', '/system/role', 'system/role/index', 'system:role:list', 1, 'TeamOutlined', 2, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '菜单管理', '/system/menu', 'system/menu/index', 'system:menu:list', 1, 'MenuOutlined', 3, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '部门管理', '/system/dept', 'system/dept/index', 'system:dept:list', 1, 'ApartmentOutlined', 4, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '岗位管理', '/system/post', 'system/post/index', 'system:post:list', 1, 'IdcardOutlined', 5, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '字典管理', '/system/dict', 'system/dict/index', 'system:dict:list', 1, 'BookOutlined', 6, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (1, '系统配置', '/system/config', 'system/config/index', 'system:config:list', 1, 'SettingOutlined', 7, 1, 1);

-- 日志管理
INSERT INTO sys_menu (parent_id, name, path, component, type, icon, sort, visible, status) 
VALUES (0, '日志管理', '/log', 'Layout', 0, 'FileTextOutlined', 2, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (9, '操作日志', '/log/oper', 'log/oper/index', 'log:oper:list', 1, 'FileSearchOutlined', 1, 1, 1);

INSERT INTO sys_menu (parent_id, name, path, component, permission, type, icon, sort, visible, status) 
VALUES (9, '登录日志', '/log/login', 'log/login/index', 'log:login:list', 1, 'LoginOutlined', 2, 1, 1);

-- 用户管理按钮权限
INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (2, '新增用户', 'system:user:add', 2, 1, 1);

INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (2, '编辑用户', 'system:user:edit', 2, 2, 1);

INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (2, '删除用户', 'system:user:delete', 2, 3, 1);

INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (2, '重置密码', 'system:user:resetPwd', 2, 4, 1);

-- 角色管理按钮权限
INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (3, '新增角色', 'system:role:add', 2, 1, 1);

INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (3, '编辑角色', 'system:role:edit', 2, 2, 1);

INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (3, '删除角色', 'system:role:delete', 2, 3, 1);

INSERT INTO sys_menu (parent_id, name, permission, type, sort, status) 
VALUES (3, '分配权限', 'system:role:permission', 2, 4, 1);
4. 分配管理员角色权限
sql
-- 用户ID为1（admin）分配角色ID为1（超级管理员）
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 超级管理员分配所有菜单权限（实际项目中应该分配全部菜单）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE del_flag = 0;
5. 插入系统配置
sql
INSERT INTO sys_config (name, `key`, value, type, group_name, is_system, status) 
VALUES ('系统名称', 'system.name', '共成设备租赁经营体管理系统', 'string', 'system', 1, 1);

INSERT INTO sys_config (name, `key`, value, type, group_name, is_system, status) 
VALUES ('系统Logo', 'system.logo', '/logo.png', 'string', 'system', 1, 1);

INSERT INTO sys_config (name, `key`, value, type, group_name, is_system, status) 
VALUES ('系统版本', 'system.version', 'v1.0.0', 'string', 'system', 1, 1);

INSERT INTO sys_config (name, `key`, value, type, group_name, is_system, status) 
VALUES ('登录失败次数限制', 'security.maxLoginAttempts', '5', 'number', 'security', 1, 1);

INSERT INTO sys_config (name, `key`, value, type, group_name, is_system, status) 
VALUES ('Token过期时间', 'security.tokenExpiration', '7200', 'number', 'security', 1, 1);
6. 插入字典数据
sql
-- 用户状态字典
INSERT INTO sys_dict_type (name, code, status, remark) 
VALUES ('用户状态', 'user_status', 1, '用户状态字典');

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (1, '启用', '1', 1, 1, 1);

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (1, '禁用', '0', 2, 0, 1);

-- 性别字典
INSERT INTO sys_dict_type (name, code, status, remark) 
VALUES ('性别', 'user_sex', 1, '用户性别字典');

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (2, '未知', '0', 1, 1, 1);

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (2, '男', '1', 2, 0, 1);

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (2, '女', '2', 3, 0, 1);

-- 操作状态字典
INSERT INTO sys_dict_type (name, code, status, remark) 
VALUES ('操作状态', 'oper_status', 1, '操作日志状态');

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (3, '成功', '1', 1, 1, 1);

INSERT INTO sys_dict_data (dict_type_id, label, value, sort, is_default, status) 
VALUES (3, '失败', '0', 2, 0, 1);
7. 插入默认部门
sql
INSERT INTO sys_dept (parent_id, name, code, leader, phone, email, sort, status) 
VALUES (0, '共成设备租赁集团', 'GONGCHENG_GROUP', '张总', '010-88888888', 'admin@gongcheng.com', 1, 1);

INSERT INTO sys_dept (parent_id, name, code, leader, phone, email, sort, status) 
VALUES (1, '技术研发部', 'TECH_DEPT', '李经理', '010-88888801', 'tech@gongcheng.com', 1, 1);

INSERT INTO sys_dept (parent_id, name, code, leader, phone, email, sort, status) 
VALUES (1, '市场销售部', 'SALES_DEPT', '王经理', '010-88888802', 'sales@gongcheng.com', 2, 1);

INSERT INTO sys_dept (parent_id, name, code, leader, phone, email, sort, status) 
VALUES (1, '人力资源部', 'HR_DEPT', '赵经理', '010-88888803', 'hr@gongcheng.com', 3, 1);

INSERT INTO sys_dept (parent_id, name, code, leader, phone, email, sort, status) 
VALUES (1, '财务部', 'FINANCE_DEPT', '孙经理', '010-88888804', 'finance@gongcheng.com', 4, 1);
四、MySQL配置文件优化
my.cnf / my.ini 推荐配置
ini
[mysqld]
# 字符集配置
character-set-server=utf8mb4
collation-server=utf8mb4_general_ci
init-connect='SET NAMES utf8mb4'

# 连接配置
max_connections=1000
max_connect_errors=100
wait_timeout=28800
interactive_timeout=28800

# 缓存配置
innodb_buffer_pool_size=2G
innodb_log_file_size=512M
innodb_log_buffer_size=256M

# 查询缓存
query_cache_type=DEMAND
query_cache_size=256M

# 其他优化
max_allowed_packet=64M
innodb_flush_log_at_trx_commit=2
sync_binlog=1
五、数据库备份与恢复脚本
备份脚本
bash
#!/bin/bash
# 备份数据库
DB_NAME="gongcheng_leasing"
DB_USER="root"
DB_PASS="123456"
BACKUP_DIR="/data/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)

mysqldump -u${DB_USER} -p${DB_PASS} --single-transaction --routines --triggers --events ${DB_NAME} > ${BACKUP_DIR}/${DB_NAME}_${DATE}.sql

# 压缩备份文件
gzip ${BACKUP_DIR}/${DB_NAME}_${DATE}.sql

# 删除30天前的备份
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +30 -delete
恢复脚本
bash
#!/bin/bash
# 恢复数据库
DB_NAME="gongcheng_leasing"
DB_USER="root"
DB_PASS="123456"
BACKUP_FILE=$1

if [ -z "$BACKUP_FILE" ]; then
    echo "请指定备份文件路径"
    exit 1
fi

mysql -u${DB_USER} -p${DB_PASS} -e "DROP DATABASE IF EXISTS ${DB_NAME};"
mysql -u${DB_USER} -p${DB_PASS} -e "CREATE DATABASE ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u${DB_USER} -p${DB_PASS} ${DB_NAME} < ${BACKUP_FILE}
六、项目中的数据库配置（Java代码）
1. 数据库配置类
java
@Configuration
@MapperScan("com.gongcheng.leasing.modules.*.mapper")
public class DataSourceConfig {
    
    @Value("${spring.datasource.url}")
    private String url;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMinimumIdle(5);
        config.setMaximumPoolSize(20);
        config.setAutoCommit(true);
        config.setIdleTimeout(30000);
        config.setPoolName("GongchengHikariCP");
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }
}
2. 数据库连接工具类
java
@Component
public class DatabaseUtils {
    
    @Autowired
    private DataSource dataSource;
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.error("数据库连接测试失败", e);
            return false;
        }
    }
    
    public String getDatabaseInfo() throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            return String.format("数据库产品: %s, 版本: %s", 
                metaData.getDatabaseProductName(), 
                metaData.getDatabaseProductVersion());
        }
    }
}
3. 数据库初始化监听器
java
@Component
public class DatabaseInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            // 检查数据库连接
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            log.info("数据库连接成功: {}", result);
            
            // 检查表是否存在
            String tableName = "sys_user";
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'gongcheng_leasing' AND TABLE_NAME = ?",
                Integer.class, tableName);
            
            if (count == 0) {
                log.warn("表 {} 不存在，请执行初始化脚本", tableName);
            } else {
                log.info("数据库初始化检查通过");
            }
        } catch (Exception e) {
            log.error("数据库初始化检查失败", e);
        }
    }
}
七、常见问题解决
1. 连接失败问题
properties
# 检查MySQL服务是否启动
net start mysql   # Windows
systemctl status mysql  # Linux

# 检查端口是否被占用
netstat -ano | findstr 3306

# 检查用户权限
GRANT ALL PRIVILEGES ON gongcheng_leasing.* TO 'root'@'localhost' IDENTIFIED BY '123456';
FLUSH PRIVILEGES;
2. 字符集乱码问题
sql
-- 查看数据库字符集
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';

-- 修改表字符集
ALTER TABLE sys_user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
3. 性能优化建议
sql
-- 分析慢查询
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;

-- 查看索引使用情况
EXPLAIN SELECT * FROM sys_user WHERE username = 'admin';

-- 优化建议：为经常查询的字段添加索引
CREATE INDEX idx_username_password ON sys_user(username, password);
八、项目配置文件示例
application.yml 完整配置
yaml
spring:
  # 数据源配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/gongcheng_leasing?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      auto-commit: true
      idle-timeout: 30000
      pool-name: GongchengHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      validation-timeout: 5000
      leak-detection-threshold: 60000

  # Redis配置（可选）
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 5000
    lettuce:
      pool:
        max-active: 8
        max-wait: -1
        max-idle: 8
        min-idle: 0

# MyBatis-Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: del_flag
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.gongcheng.leasing.modules.*.entity

# JWT配置
jwt:
  secret: GongchengLeasing2026SecretKey
  expiration: 7200000  # 2小时
  header: Authorization
  prefix: Bearer
文档版本: v1.0
创建日期: 2026-07-27
适用项目: 共成设备租赁经营体后台管理系统
数据库版本: MySQL 8.0+


# 共成设备租赁经营体后台管理系统 - 项目架构设计提示词

## 一、项目整体架构模式推荐

### 1. 后端架构模式：分层架构 + DDD（领域驱动设计）混合模式
┌─────────────────────────────────────────────────────────────┐
│ 表现层 (Presentation Layer) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Controller层 - REST API接口 │ │
│ │ - 参数验证 │ │
│ │ - 统一响应封装 │ │
│ │ - 异常捕获 │ │
│ └──────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ 应用层 (Application Layer) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Service层 - 业务逻辑处理 │ │
│ │ - 事务管理 │ │
│ │ - 业务编排 │ │
│ │ - 缓存处理 │ │
│ │ - 权限控制 │ │
│ └──────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ DTO层 - 数据传输对象 │ │
│ │ - Request DTO │ │
│ │ - Response DTO │ │
│ └──────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ 领域层 (Domain Layer) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Entity/Model - 业务实体 │ │
│ │ - 业务规则 │ │
│ │ - 领域事件 │ │
│ └──────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ 基础设施层 (Infrastructure Layer) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Repository/Mapper - 数据访问 │ │
│ │ - MyBatis-Plus │ │
│ │ - 自定义SQL │ │
│ └──────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ 外部服务调用 │ │
│ │ - Redis缓存 │ │
│ │ - 消息队列 │ │
│ │ - 第三方API │ │
│ └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

text

### 2. 前端架构模式：MVVM + 组件化 + 状态管理模式
┌─────────────────────────────────────────────────────────────┐
│ View层 (视图层) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ React组件 - 页面/组件视图 │ │
│ │ - 页面级组件 │ │
│ │ - 功能级组件 │ │
│ │ - 通用级组件 │ │
│ └──────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ ViewModel层 (视图模型层) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Hooks - 自定义钩子 │ │
│ │ - 状态管理 │ │
│ │ - 副作用处理 │ │
│ │ - 逻辑复用 │ │
│ └──────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ Store - 全局状态管理 │ │
│ │ - Redux Toolkit │ │
│ │ - 状态集中管理 │ │
│ └──────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ Model层 (数据模型层) │
│ ┌──────────────────────────────────────────────────────┐ │
│ │ API Service - 接口服务 │ │
│ │ - 网络请求 │ │
│ │ - 数据转换 │ │
│ │ - 错误处理 │ │
│ └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

text

---

## 二、完整项目架构设计提示词

### 【系统架构设计提示词】

```markdown
# 共成设备租赁经营体后台管理系统 - 完整架构设计

## 一、项目概述
开发一个基于前后端分离架构的房地产后台管理系统，采用微服务架构思想设计，具备高可用、高扩展性、安全可靠的特点。

## 二、技术架构选型

### 后端技术架构
1. **基础框架**: Spring Boot 2.7.x (采用工厂模式、模板方法模式)
2. **安全框架**: Spring Security + JWT (策略模式)
3. **ORM框架**: MyBatis-Plus 3.5.x (采用建造者模式)
4. **数据库**: MySQL 8.0 + Redis 7.0 (读写分离模式)
5. **连接池**: HikariCP (工厂模式)
6. **日志框架**: Logback + SLF4J (门面模式)
7. **工具库**: Hutool (工具类模式)
8. **API文档**: Knife4j (Swagger增强)
9. **任务调度**: Quartz (观察者模式)
10. **消息队列**: RabbitMQ (发布订阅模式) - 可选

### 前端技术架构
1. **UI框架**: React 18.x + Ant Design 5.x (组件模式)
2. **状态管理**: Redux Toolkit (单一状态树模式)
3. **路由管理**: React Router v6 (路由模式)
4. **HTTP客户端**: Axios (代理模式)
5. **构建工具**: Vite 4.x (插件模式)
6. **CSS方案**: CSS-in-JS (styled-components)
7. **代码规范**: ESLint + Prettier + Husky
8. **国际化**: react-i18next (装饰器模式)

## 三、设计模式应用

### 后端设计模式
1. **工厂模式**: 用于创建各种Service、Mapper实例
2. **单例模式**: Spring Bean默认作用域
3. **代理模式**: Spring AOP实现日志、权限控制
4. **模板方法模式**: MyBatis-Plus BaseMapper
5. **策略模式**: 权限验证、数据源切换
6. **观察者模式**: 事件监听、日志记录
7. **装饰器模式**: 缓存装饰、权限装饰
8. **责任链模式**: 过滤器链、拦截器链

### 前端设计模式
1. **组件模式**: React组件化开发
2. **容器/展示组件模式**: 分离逻辑和视图
3. **高阶组件模式**: 权限控制、日志记录
4. **渲染属性模式**: 通用功能复用
5. **Hooks模式**: 状态逻辑复用
6. **代理模式**: API请求拦截
7. **观察者模式**: Redux状态订阅
8. **工厂模式**: 创建不同类型的组件

## 四、项目分层架构

### 后端分层（自上而下）
┌─────────────────────────────────────┐
│ Controller层 (接口层) │
│ - @RestController │
│ - 参数校验 │
│ - 统一返回 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Service层 (业务逻辑层) │
│ - 接口+实现类 │
│ - 事务管理 │
│ - 缓存处理 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Manager层 (通用业务层) │
│ - 通用业务处理 │
│ - 工具类服务 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Mapper层 (数据访问层) │
│ - 数据库CRUD │
│ - 自定义SQL │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ Entity层 (实体层) │
│ - 数据库映射 │
│ - 业务实体 │
└─────────────────────────────────────┘

text

### 前端分层（自上而下）
┌─────────────────────────────────────┐
│ 页面层 (Pages) │
│ - 功能页面 │
│ - 路由配置 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ 组件层 (Components) │
│ - 业务组件 │
│ - 通用组件 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ 状态管理层 (Store) │
│ - Redux Slices │
│ - 全局状态 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ 服务层 (Services) │
│ - API调用 │
│ - 数据转换 │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ 工具层 (Utils) │
│ - 通用工具函数 │
│ - 自定义Hooks │
└─────────────────────────────────────┘

text

## 五、包命名规范

### 后端包结构
```java
com.gongcheng.leasing
├── common                    // 公共模块
│   ├── annotation            // 自定义注解
│   ├── aspect                // AOP切面
│   ├── config                // 配置类
│   ├── constant              // 常量类
│   ├── enums                 // 枚举类
│   ├── exception             // 异常处理
│   ├── handler               // 处理器
│   ├── interceptor           // 拦截器
│   ├── listener              // 监听器
│   └── utils                 // 工具类
├── modules                   // 业务模块
│   ├── auth                  // 认证模块
│   │   ├── controller
│   │   ├── service
│   │   ├── mapper
│   │   ├── entity
│   │   ├── dto
│   │   └── vo
│   ├── system                // 系统管理
│   │   ├── user
│   │   ├── role
│   │   ├── menu
│   │   └── dept
│   ├── business              // 业务模块
│   │   ├── house             // 房源管理
│   │   ├── customer          // 客户管理
│   │   └── contract          // 合同管理
│   └── monitor               // 监控模块
│       ├── log
│       └── online
└── generator                 // 代码生成器
    ├── template
    └── config
前端包结构
javascript
src/
├── api/                      // API接口
│   ├── modules/              // 模块化接口
│   │   ├── auth.js
│   │   ├── user.js
│   │   └── house.js
│   └── index.js              // 统一导出
├── assets/                   // 静态资源
│   ├── images/
│   ├── fonts/
│   └── styles/
├── components/               // 组件
│   ├── business/             // 业务组件
│   │   ├── HouseCard/
│   │   └── CustomerForm/
│   └── common/               // 通用组件
│       ├── Layout/           // 含 Header / Sidebar / TagsView / BreadcrumbNav / Footer
│       ├── Table/
│       └── SearchForm/
├── hooks/                    // 自定义Hooks
│   ├── useAuth.js
│   ├── useTable.js
│   └── usePermission.js
├── pages/                    // 页面
│   ├── Login/
│   ├── Dashboard/
│   ├── System/
│   └── Business/
├── router/                   // 路由
│   ├── index.js
│   └── permission.js
├── store/                    // 状态管理
│   ├── modules/
│   │   ├── user.js
│   │   ├── app.js
│   │   ├── permission.js
│   │   └── tagsView.js       // 路由多页签
│   └── index.js
├── utils/                    // 工具
│   ├── request.js
│   ├── storage.js
│   ├── menu.js               // 面包屑/菜单标题解析
│   └── validator.js
└── constants/                // 常量
    ├── index.js
    └── enums.js
六、接口设计规范
RESTful API设计原则
统一使用复数名词: /api/users, /api/roles

HTTP方法语义化:

GET: 查询

POST: 新增

PUT: 更新

DELETE: 删除

状态码规范:

200: 成功

400: 参数错误

401: 未授权

403: 禁止访问

404: 资源不存在

500: 服务器错误

统一响应格式
json
{
  "code": 200,
  "msg": "操作成功",
  "data": {},
  "timestamp": 1700000000000,
  "traceId": "uuid-trace-id"  // 用于链路追踪
}
分页请求格式
json
{
  "pageNum": 1,
  "pageSize": 10,
  "orderBy": "create_time",
  "orderDir": "desc",
  "keyword": "搜索关键词",
  "filters": {
    "status": 1,
    "deptId": 100
  }
}
七、代码生成器架构
使用MyBatis-Plus代码生成器
java
// 代码生成器配置
public class CodeGenerator {
    public static void main(String[] args) {
        // 1. 配置数据源
        // 2. 配置全局策略
        // 3. 配置包名
        // 4. 配置策略
        // 5. 生成代码
    }
}
模板引擎
使用Freemarker/Velocity模板

自定义模板生成Controller/Service/Mapper/Entity

支持DTO/VO自动生成

支持Swagger注解自动添加

八、安全架构设计
认证授权流程
text
1. 用户登录 → 验证用户名密码
2. 生成JWT Token → 返回给前端
3. 前端携带Token请求接口
4. 后端验证Token → 解析用户信息
5. 权限校验 → 通过/拒绝
6. 返回业务数据
安全防护策略
SQL注入防护: MyBatis参数绑定

XSS防护: 输入过滤 + 输出编码

CSRF防护: Token + SameSite

接口限流: Redis + 令牌桶

密码加密: BCrypt

HTTPS强制: 生产环境强制HTTPS

九、缓存架构设计
多级缓存策略
text
┌─────────────┐
│  前端缓存    │ (localStorage/sessionStorage)
├─────────────┤
│  Redis缓存   │ (分布式缓存)
├─────────────┤
│  本地缓存    │ (Ehcache/Caffeine)
├─────────────┤
│  数据库      │ (持久化存储)
└─────────────┘
缓存使用规范
用户信息缓存: Redis存储，过期时间1小时

权限数据缓存: Redis存储，用户登录时加载

配置数据缓存: Redis存储，系统启动加载

业务数据缓存: 根据业务场景决定过期时间

十、监控与运维架构
日志体系
text
┌─────────────────────────────────────┐
│  访问日志 (Access Log)              │
│  - 请求URL、IP、耗时                │
├─────────────────────────────────────┤
│  业务日志 (Business Log)            │
│  - 操作记录、业务状态               │
├─────────────────────────────────────┤
│  错误日志 (Error Log)               │
│  - 异常堆栈、错误信息               │
├─────────────────────────────────────┤
│  性能日志 (Performance Log)         │
│  - SQL耗时、接口耗时                │
└─────────────────────────────────────┘
监控指标
系统指标: CPU、内存、磁盘、网络

应用指标: QPS、响应时间、错误率

业务指标: 用户量、交易量、转化率

数据库指标: 连接数、慢查询、死锁

十一、数据库设计原则
命名规范
表名: 小写+下划线，如 sys_user, business_house

字段名: 小写+下划线，如 create_time, user_name

主键: id，自增BIGINT类型

索引命名: idx_字段名, uk_字段名

外键: 不使用物理外键，逻辑关联

字段设计规范
sql
-- 必备字段
id          BIGINT PRIMARY KEY AUTO_INCREMENT
create_by   BIGINT COMMENT '创建人'
create_time DATETIME DEFAULT CURRENT_TIMESTAMP
update_by   BIGINT COMMENT '更新人'
update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
del_flag    TINYINT DEFAULT 0 COMMENT '删除标记'
remark      VARCHAR(500) COMMENT '备注'
索引设计原则
主键索引: 默认

唯一索引: 唯一性字段

普通索引: 查询频繁字段

联合索引: 多条件查询

避免过多索引: 影响写入性能

十二、部署架构
环境分离
text
开发环境 (dev) → 测试环境 (test) → 预发布环境 (pre) → 生产环境 (prod)
Docker容器化部署
text
┌─────────────────────────────────────┐
│  Nginx (静态资源 + 反向代理)        │
├─────────────────────────────────────┤
│  Spring Boot Application (后端)     │
├─────────────────────────────────────┤
│  Redis (缓存)                       │
├─────────────────────────────────────┤
│  MySQL (数据库)                     │
├─────────────────────────────────────┤
│  RabbitMQ (消息队列) - 可选         │
└─────────────────────────────────────┘
CI/CD流程
代码提交: Git push

代码检查: SonarQube扫描

单元测试: JUnit + Mockito

构建打包: Maven/Gradle

镜像构建: Docker Build

自动化部署: Jenkins/GitLab CI

【AI编程助手使用规范】
一、通用限制条件
1. 代码生成限制
text
- 单次生成代码行数不超过500行
- 复杂功能需分步生成
- 不得生成未经测试的SQL语句
- 不得生成硬编码敏感信息
- 每次生成需添加详细注释
- 需遵循阿里巴巴Java开发手册
- 前端需遵循Airbnb React编码规范
2. 附件默认条件
text
默认附件清单：
1. 项目技术栈说明文档
2. 数据库ER图
3. API接口文档
4. 配置文件模板
5. 代码规范文档

如未特别说明，则使用以下默认配置：
- 前端UI框架: Ant Design 5.x
- 后端框架: Spring Boot 2.7.x
- ORM: MyBatis-Plus 3.5.x
- 数据库: MySQL 8.0
- 字符集: utf8mb4
- JDK版本: 1.8
- 构建工具: Maven 3.8.x
- 前端构建: Vite 4.x
- 状态管理: Redux Toolkit
二、特定场景限制
1. 前端开发限制
text
- 组件必须使用函数式组件 + Hooks
- 样式优先使用styled-components或CSS Modules
- 如未指定UI框架，默认使用Ant Design 5.x组件库
- 图标统一使用Ant Design图标库
- 表单必须使用Form组件 + 表单验证
- 列表必须使用Table组件 + 分页
- 路由必须使用React Router v6
- 状态管理必须使用Redux Toolkit
- API请求必须通过统一的Axios实例
- 错误处理必须有全局错误边界
2. 后端开发限制
text
- Controller层必须使用@RestController
- Service层必须有接口和实现类
- 必须使用@Transactional管理事务
- 参数校验必须使用@Valid + 分组验证
- 必须使用自定义异常 + 全局异常处理
- 日志记录必须使用@Slf4j
- 配置文件必须支持多环境
- 敏感信息必须加密存储
- 接口文档必须使用Swagger注解
- 必须使用统一响应格式
3. 数据库限制
text
- 表名必须使用小写+下划线命名
- 字段名必须使用小写+下划线命名
- 主键统一使用id
- 必须包含create_time, update_time
- 必须包含del_flag逻辑删除字段
- 字符集统一使用utf8mb4
- 存储引擎统一使用InnoDB
- 不允许使用存储过程
- 不允许使用视图
- 不允许使用外键约束
三、附加条件设置
1. 代码质量要求
yaml
代码规范:
  - 注释覆盖率不低于30%
  - 核心业务逻辑必须有单元测试
  - 必须通过静态代码扫描
  - 需包含异常处理
  - 需包含日志记录
  
性能要求:
  - 接口响应时间 < 500ms
  - 单表查询 < 100ms
  - 分页查询 < 200ms
  - 并发支持 > 100 QPS
  
安全要求:
  - 密码必须BCrypt加密
  - JWT过期时间 < 2小时
  - 敏感数据脱敏
  - SQL注入防护
  - XSS防护
2. 文档要求
yaml
必须包含的文档:
  - 接口文档 (Swagger/Postman)
  - 数据库设计文档
  - 部署文档
  - 用户手册
  
文档格式要求:
  - Markdown格式
  - 包含示例代码
  - 包含使用说明
  - 包含常见问题
3. 样式默认配置
css
/* 如未指定样式，使用Ant Design默认主题 */
--primary-color: #1890ff;
--success-color: #52c41a;
--warning-color: #faad14;
--error-color: #ff4d4f;
--font-size-base: 14px;
--border-radius-base: 6px;

/* 响应式断点 */
--screen-xs: 480px;
--screen-sm: 576px;
--screen-md: 768px;
--screen-lg: 992px;
--screen-xl: 1200px;
--screen-xxl: 1600px;
四、生成代码模板
1. 后端Controller模板
java
/**
 * ${moduleName}管理Controller
 *
 * @author ${author}
 * @date ${date}
 */
@RestController
@RequestMapping("/api/${moduleName}")
@Api(tags = "${moduleName}管理")
@Slf4j
@RequiredArgsConstructor
public class ${ClassName}Controller {

    private final ${ClassName}Service ${className}Service;

    @PostMapping("/list")
    @ApiOperation("分页查询")
    public Result<PageResult<${ClassName}VO>> queryPage(@RequestBody @Valid ${ClassName}PageDTO pageDTO) {
        PageResult<${ClassName}VO> page = ${className}Service.queryPage(pageDTO);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询详情")
    public Result<${ClassName}VO> queryDetail(@PathVariable Long id) {
        ${ClassName}VO vo = ${className}Service.queryDetail(id);
        return Result.success(vo);
    }

    @PostMapping
    @ApiOperation("新增")
    @RequiresPermissions("${moduleName}:add")
    public Result<Void> add(@RequestBody @Valid ${ClassName}AddDTO addDTO) {
        ${className}Service.add(addDTO);
        return Result.success();
    }

    @PutMapping("/{id}")
    @ApiOperation("更新")
    @RequiresPermissions("${moduleName}:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ${ClassName}UpdateDTO updateDTO) {
        updateDTO.setId(id);
        ${className}Service.update(updateDTO);
        return Result.success();
    }

    @DeleteMapping("/{ids}")
    @ApiOperation("删除")
    @RequiresPermissions("${moduleName}:delete")
    public Result<Void> delete(@PathVariable Long[] ids) {
        ${className}Service.delete(ids);
        return Result.success();
    }
}
2. 前端页面模板
jsx
/**
 * ${moduleName}管理页面
 *
 * @author ${author}
 * @date ${date}
 */
import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Space, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getList, add, update, delete } from '@/api/modules/${moduleName}';
import styles from './index.module.css';

const ${ClassName}Page = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });
  const [modalVisible, setModalVisible] = useState(false);
  const [editData, setEditData] = useState(null);
  const [form] = Form.useForm();

  // 查询列表
  const fetchList = async (params = {}) => {
    setLoading(true);
    try {
      const res = await getList({
        ...pagination,
        ...params
      });
      setDataSource(res.records);
      setPagination({
        ...pagination,
        total: res.total
      });
    } catch (error) {
      message.error('查询失败');
    } finally {
      setLoading(false);
    }
  };

  // 新增/编辑
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      if (editData) {
        await update(editData.id, values);
        message.success('更新成功');
      } else {
        await add(values);
        message.success('新增成功');
      }
      setModalVisible(false);
      fetchList();
    } catch (error) {
      console.error('保存失败', error);
    }
  };

  // 删除
  const handleDelete = (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该数据吗？删除后不可恢复！',
      onOk: async () => {
        await delete([id]);
        message.success('删除成功');
        fetchList();
      }
    });
  };

  // 表格列配置
  const columns = [
    {
      title: '序号',
      dataIndex: 'index',
      render: (_, __, index) => (pagination.current - 1) * pagination.pageSize + index + 1
    },
    // 业务列配置...
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.id)}>
            删除
          </Button>
        </Space>
      )
    }
  ];

  return (
    <div className={styles.container}>
      {/* 搜索区域 */}
      <div className={styles.searchArea}>
        <Form layout="inline" onFinish={(values) => fetchList(values)}>
          {/* 搜索条件 */}
          <Form.Item>
            <Button type="primary" htmlType="submit">
              搜索
            </Button>
            <Button onClick={() => form.resetFields()}>重置</Button>
          </Form.Item>
        </Form>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          新增
        </Button>
      </div>

      {/* 表格 */}
      <Table
        loading={loading}
        dataSource={dataSource}
        columns={columns}
        pagination={pagination}
        onChange={(pagination) => {
          setPagination(pagination);
          fetchList({ ...pagination });
        }}
        rowKey="id"
      />

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editData ? '编辑' : '新增'}
        open={modalVisible}
        onOk={handleSave}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          {/* 表单字段 */}
        </Form>
      </Modal>
    </div>
  );
};

export default ${ClassName}Page;
使用说明
如何向AI编程助手发送请求
标准请求模板：
text
请根据以下规范生成代码：

【项目背景】
- 项目名称: 共成设备租赁经营体后台管理系统
- 技术栈: React + Ant Design + Spring Boot + MyBatis-Plus + MySQL

【功能需求】
- 模块名称: 用户管理
- 功能描述: 实现用户的CRUD操作、分页查询、状态切换、密码重置

【架构要求】
- 后端: 使用分层架构 (Controller → Service → Mapper → Entity)
- 前端: 使用组件化开发 (Page → Components → Store → API)
- 设计模式: 工厂模式、代理模式、策略模式

【代码要求】
- 遵循阿里巴巴Java开发规范
- 前端遵循Airbnb React规范
- 包含完整注释和异常处理
- 使用统一响应格式

【默认配置】
- 如未指定UI框架，使用Ant Design 5.x
- 如未指定数据库，使用MySQL 8.0
- 如未指定缓存，使用Redis 7.0
- 字符集: utf8mb4

【附件文档】
- 数据库设计文档 (见附件)
- API接口文档 (见附件)
- 技术架构文档 (见附件)
文档版本: v1.0
创建日期: 2026-07-27
适用项目: 共成设备租赁经营体后台管理系统
文档状态: 正式版

text

这份提示词文档提供了完整的架构设计模式、技术选型、代码规范和AI编程助手使用规范，您可以直接复制使用其中的模板和规范来指导AI编程助手生成符合项目标准的代码。

text

这份数据库配置提示词包含了完整的数据库创建、表结构设计、初始化数据、配置优化等内容，您可以直接使用这些脚本快速搭建项目数据库环境。


text

这份提示词文档涵盖了完整的后台管理系统开发所需的所有关键要素，您可以根据实际项目需求进行增删改。建议将此文档作为项目的技术规范和开发指南使用。



