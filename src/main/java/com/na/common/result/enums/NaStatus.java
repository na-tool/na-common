package com.na.common.result.enums;

import com.na.common.utils.NaLocalLangUtil;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public enum NaStatus implements INaStatusProvider {

    // 默认
    DEFAULT(0, "default", "默认", "default"),
    ERROR(-1, "error", "失败", "error"),
    EXCEPTION(-2, "exception", "异常", "exception"),
    SUCCESS(1, "success", "成功", "success"),
    TEST(999, "test", "测试", "test"),

    // 权限验证 & 服务器
    USER_NEED_AUTHORITIES(401, "please login", "未登录", "unauthorized"),
    USER_NO_ACCESS(403, "access denied", "无权访问", "forbidden"),
    SERVER_ERROR(500, "server error", "服务器错误", "server.error"),
    USER_AUTH_EXPIRED(401, "The authorization is invalid, please re-authorize", "授权失效，请重新授权", "auth.expired"),
    USER_AUTHORITIES_EXPIRED(401, "The authorization has expired. Please log out and log in again.", "授权过期，请退出重新登陆!", "auth.token.expired"),

    // 通用内容
    INTERNAL_SERVER_ERROR_ARGS(10000, "Internal Server Error: {0}", "服务端异常: {0}", "server.error.args"),
    PARAM_PAGE_NO_NOT_VALID(10001, "param pageNo not valid", "参数pageNo无效", "param.pageNo.invalid"),
    PARAM_PAGE_SIZE_NOT_VALID(10002, "param pageSize not valid", "参数pageSize无效", "param.pageSize.invalid"),
    RETRY_AFTER_REFRESH(10003, "RETRY_AFTER_REFRESH", "刷新后重试", "retry.after.refresh"),
    PARAMS_NOT_VALID(10004, "params not valid, please check it and try again!", "参数无效，请检查后后重试！", "param.invalid"),
    PARAMS_FORMAT_ERROR(10004, "Error in parameter format. Please check and try again!", "参数格式错误，请检查后重试！", "param.format.error"),
    OVER_UP_LIMIT(10005, "Exceed the upper limit, please modify it and try again!", "超出上限，请修改后重新尝试！", "over.limit"),
    PARSING_FAILED(10006, "Parsing failed, please modify it and try again!", "解析失败，请修改后重新尝试！", "parsing.failed"),
    OPT_OVER_AUTH(10007, "The operation exceeds the authorization range, please contact the administrator!", "操作超出授权范围，请联系管理员！", "auth.limit.exceeded"),
    AUTHORIZATION_EXPIRED(10008, "The authorization has expired. Please log out and log in again.", "授权过期，请联系相关人员!", "auth.expired.general"),
    FAIL_REQUEST_REPETITION(10009, "The request is too fast, please try again later.", "请求太快了,请稍后重试！", "repeat.request"),

    UN_BIND_USER_TENANT(10005, "Please unbind the advanced line and try again.", "请先进行解绑后重试！", "tenant.unbind.required"),
    SYS_DEF_DATA_BAN_OPT(10008, "There is a system default data prohibiting operation.", "存在系统默认数据禁止操作", "system.default.operation.forbidden"),
    BAN_DELETE(10008, "Exist ban delete data, Please check!", "存在禁止删除数据，请检查！", "delete.ban"),
    BAN_OPT(10008, "No operation data exists. Please check!", "存在禁止操作数据，请检查！", "operation.ban"),

    // 登录相关
    LOGIN_FAILED(20001, "login failed, Please check and try again or contact the administrator", "登录失败，请检查后重试或联系管理员！", "login.failed"),
    LOGIN_FAILED_USER_NOT_AUTH(20002, "Login failed, the user is not authorized, please contact the administrator!", "登录失败，用户未授权请联系管理员！", "login.user.not.auth"),

    // 租户相关
    TENANT_NOT_EXIST(30000, "tenant not exist", "租户不存在，请联系管理员", "tenant.not.exist"),
    TENANT_ALREADY_EXIST(30001, "tenant already exists, please modify it and try again!", "租户已存在，请修改后重新尝试！", "tenant.exists"),
    TENANT_NOT_ENABLE(30002, "tenant not enable, please contact the administrator!", "租户未开启，请与管理员联系！", "tenant.not.enabled"),
    TENANT_NOT_MATCH(30003, "tenant not match", "租户不匹配，请联系管理员！", "tenant.not.match"),
    TENANT_AUTH_ERROR(30004, "tenant auth error", "租户认证错误，请联系管理员！", "tenant.auth.error"),
    TENANT_BIND_USER(30005, "The tenant has bound the user, please unbind it first!", "租户已经绑定用户，请先进行解绑！", "tenant.bind.user"),

    // 用户相关
    USER_NOT_EXIST(40000, "user not exist", "用户不存在，请联系管理员", "user.not.exist"),
    USER_ALREADY_EXIST(40001, "user already exists, please modify it and try again!", "用户已存在，请修改后重新尝试！", "user.exists"),
    USER_NOT_ENABLE(40002, "user not enable, please contact the administrator!", "用户未开启，请与管理员联系！", "user.not.enabled"),
    USER_ALREADY_BIND_TENANT(40003, "The user is bound tenant, please unbind and try again!", "用户被绑定租户，请解绑后重试！", "user.bind.tenant"),
    USER_ALREADY_BIND_ROLE(40004, "The user is bound role, please unbind and try again!", "用户被绑定角色，请解绑后重试！", "user.bind.role"),
    USER_AUTH_ERROR(40005, "user auth error", "用户认证错误，请联系管理员！", "user.auth.error"),
    USER_PARSING_FAILED(40006, "user parsing failed", "用户解析失败，请联系管理员！", "user.parsing.failed"),

    // 角色相关
    ROLE_ALREADY_EXIST(50000, "role already exists, please modify it and try again!", "角色已存在，请修改后重新尝试！", "role.exists"),
    ROLE_ALREADY_BIND_USER(50001, "role already bind user, please modify it and try again!", "角色已被用户绑定，请修改后重新尝试！", "role.bind.user"),
    ROLE_ALREADY_BIND_MENU(50002, "role already bind menu, please modify it and try again!", "角色已被路由绑定，请修改后重新尝试！", "role.bind.menu"),
    ROLE_ALREADY_DEFAULT(50002, "role already default not delete, please modify it and try again!", "角色存在默认角色不能删除，请修改后重新尝试！", "role.default.cannot.delete"),
    ROLE_NOT_EXIST(50003, "role not exists, please modify it and try again!", "角色不存在，请修改后重新尝试！", "role.not.exist"),

    // 权限资源相关
    AUTH_RESOURCE_ALREADY_DEFAULT(60000, "There are system default permission resources, it is forbidden to delete, please check or contact the administrator and try again!", "存在系统默认权限资源，禁止删除，请检查或联系管理员后重新尝试！", "auth.resource.default"),
    AUTH_RESOURCE_ALREADY_EXIST(60001, "auth resource already exist, please modify it and try again!", "资源权限已存在，请修改后重新尝试！", "auth.resource.exists"),
    AUTH_RESOURCE_NOT_EXIST(60002, "auth resource not exist, please modify it and try again!", "资源权限不存在，请修改后重新尝试！", "auth.resource.not.exist"),

    // 路由菜单相关
    MENU_ALREADY_EXIST(70000, "menu already exist, please modify it and try again!", "菜单路由已存在，请修改后重新尝试！", "menu.exists"),
    MENU_NOT_EXIST(70001, "menu not exist, please modify it and try again!", "菜单路由不存在，请修改后重新尝试！", "menu.not.exist"),
    MENU_ALREADY_BIND_ROLE(70002, "menu already bind role, please modify it and try again!", "路由已被角色绑定，请修改后重新尝试！", "menu.bind.role"),
    MENU_ALREADY_ALREADY_DEFAULT(70002, "There is a system default menu route, it is forbidden to delete, please check or contact the administrator and try again!", "存在系统默认菜单路由，禁止删除，请检查或联系管理员后重新尝试！", "menu.default.cannot.delete"),
    MENU_ALREADY_BIND_TENANT(70003, "menu already bind tenant, please modify it and try again!", "菜单已被租户绑定，请修改后重新尝试！", "menu.bind.tenant"),
    MENU_PARENT_NOT_EXIST(70004, "menu parent not exist, please modify it and try again!", "菜单路由父级不存在，请修改后重新尝试！", "menu.parent.not.exist"),
    MENU_PARENT_CHOOSE_ERROR(70005, "menu parent choose error, please modify it and try again!", "菜单路由父级选择错误，请修改后重新尝试！", "menu.parent.choose.error"),

    // 任务相关
    JOB_TASK_NOT_EXIST(90000, "job task not exist", "任务不存在，请联系管理员", "job.task.not.exist"),
    JOB_TASK_ALREADY_EXIST(90001, "job task already exist, please modify it and try again!", "系统任务已存在，请修改后重新尝试！", "job.task.exists"),
    JOB_TASK_ALREADY_START(90002, "The system task has been opened, please close it and try again!", "系统任务已开启，请关闭后，重新尝试！", "job.task.started"),
    JOB_TASK_MOT_EXIST(90002, "job task not exist, please modify it and try again!", "系统任务不存在，请修改后重新尝试！", "job.task.not.exist.again"),

    DEPT_ALREADY_EXIST(100000, "department already exist", "部门已存在，请修改后重新尝试！", "dept.exists"),
    DEPT_NOT_EXIST(100001, "department not exist", "部门不存在，请修改后重新尝试！", "dept.not.exist");



    ;
    private final Integer code;
    private final String enMsg;
    private final String zhMsg;

    private final String msgKey;

    NaStatus(Integer code, String enMsg, String zhMsg, String msgKey)  {
        this.code = code;
        this.enMsg = enMsg;
        this.zhMsg = zhMsg;
        this.msgKey = msgKey;
    }


    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getEnMsg() {
        return enMsg;
    }

    @Override
    public String getZhMsg() {
        return zhMsg;
    }

    @Override
    public String getMsg() {
        if (Locale.SIMPLIFIED_CHINESE.getLanguage().equals(LocaleContextHolder.getLocale().getLanguage())) {
            return this.zhMsg;
        } else {
            return this.enMsg;
        }
    }

    @Override
    public String msgKey() {
        return msgKey;
    }

    @Override
    public String getLanguageMsg() {
        return NaLocalLangUtil.get(msgKey);  // 使用 msgKey 获取翻译
    }


}
