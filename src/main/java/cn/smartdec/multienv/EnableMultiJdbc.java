package cn.smartdec.multienv;

import cn.smartdec.multienv.autoconfigure.jdbc.MultiJdbcAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({MultiJdbcAutoConfiguration.class})
public @interface EnableMultiJdbc {

}
