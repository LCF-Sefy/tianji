import com.tianji.common.utils.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class test {

    @Test
    public void test(){
        LocalDateTime now = LocalDateTime.now();
        long epochMilli = DateUtils.toEpochMilli(now);
        System.out.println("epochMilli = " + epochMilli);
    }
}
