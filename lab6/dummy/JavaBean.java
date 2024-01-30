package dummy;

import java.io.Serializable;

/**
 * @Author 3590
 * @Date 2024/1/30 21:28
 * @Description
 */
public class JavaBean implements Serializable {
    public String name;
    public Integer age;
    public Integer id;

    public JavaBean(String name, Integer age, Integer id) {
        this.name = name;
        this.age = age;
        this.id = id;
    }

    @Override
    public String toString() {
        return "JavaBean{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", id=" + id +
                '}';
    }
}
