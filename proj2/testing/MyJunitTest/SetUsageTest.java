package testing.MyJunitTest;

import jdk.jshell.execution.Util;
import org.junit.Test;

import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @Author 3590
 * @Date 2024/2/5 23:17
 * @Description
 */
public class SetUsageTest {
    @Test
    public void test() {
        Set<Integer> set = new HashSet<>();
        set.add(5);
        set.add(6);

        set.forEach(System.out::println);
    }

    @Test
    public void test2() {
        String hexString = "8f94139338f9404f26296befa88755fc2598c289";
        String substring = hexString.substring(0, 2);
        System.out.println(substring);
    }

    @Test
    public void test3() {
        Map<String, String> map = new HashMap<>();
        map.put("kefeng", "1");
        map.put("lida", "2");
        Map<String, String> map2 = new HashMap<>();
        map.put("kefeng", "1");
        map.put("lida--", "2");
        Set<String> set = map.keySet();
        set.addAll(map2.keySet());
        System.out.println(set);
    }
}