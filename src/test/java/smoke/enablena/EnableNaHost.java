package smoke.enablena;

import com.na.common.annotation.EnableNA;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 宿主启动类：使用 {@code @EnableNA}。
 *
 * <p>用于验证"宿主仍标注 {@code @EnableNA} ＋ 自动装配同时生效"不会重复注册、
 * 也不会抛 {@code BeanDefinitionOverrideException}。
 *
 * <p>刻意放在独立子包：若与另一个 {@code @SpringBootApplication} 同包，
 * 两个启动类会互相组件扫描到对方，干扰验证。
 */
@EnableNA
@SpringBootApplication
public class EnableNaHost {
}
