package smoke.nouser;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 宿主启动类：<b>不提供</b> {@code INoRepeatSubmitUserService}。
 *
 * <p>用于验证"只想用 IP 维度防重复提交"的场景：即使开启了 {@code na.submit.enable}，
 * 也不会因为缺少用户标识 SPI 而启动失败 —— 只注册 IP 维度的切面，
 * 用户维度的切面跳过并告警。
 *
 * <p>刻意放在独立子包：若与 {@code smoke.NaCommonSmoke.HostApplication} 同包，
 * 后者提供的 SPI 桩会被扫描进来，验证就失去意义。
 */
@SpringBootApplication
public class NoUserServiceHost {
}
