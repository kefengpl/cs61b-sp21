package testing.othertest;

import dummy.JavaBean;
import net.sf.saxon.trans.SymbolicName;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Paths;

/**
 * @Author 3590
 * @Date 2024/1/30 20:16
 * @Description Java 文件基础
 * File file  对象表示可能存在的 filename
 * 流的API：四个抽象基类： (字节流，非文本文件)InputStream OutputStream (字符流，文本文件)Reader Writer
 *
 * 常用节点流：节点流（Node Stream）：节点流直接与特定的数据源或数据目标连接，用于从中读取或写入数据。
 * 文件流： FileInputStream、FileOutputStream、FileReader、FileWriter
 * 字节/字符数组流： ByteArrayInputStream、ByteArrayOutputStream、CharArrayReader、CharArrayWriter
 *
 * 处理流：处理流不直接连接到数据源或目标。它们附加到一个已存在的流（节点流或另一个处理流）上，通过提供一些额外的功能来处理数据。
 * 缓冲流，增加缓冲功能，避免频繁读写硬盘，进而提升读写效率。：BufferedInputStream、BufferedOutputStream、BufferedReader、BufferedWriter
 * 转换流，实现字节流和字符流之间的转换。：InputStreamReader、OutputStreamReader
 * 对象流，提供直接读写 Java 对象功能：ObjectInputStream、ObjectOutputStream
 * 缓冲区流：当缓冲区满了(8192KB)或调用flush()才会写入文件。
 *
 * java 区分 char 和 byte， byte 是 C 中的 char，字节；byte 是字符
 * 提示：所有的流都必须关闭，先关闭外层，再关闭内层
 */
public class MyTest {
    @Test
    public void test() { // 获取当前工作目录 D:\study\Git Repo\cs61b\cs61b-sp21\lab6
        String property = System.getProperty("user.dir");
        System.out.println(property);
    }

    @Test
    public void test1() throws Exception {
        File file = new File("dummy.txt");
        boolean newFile = file.createNewFile();
        Class<?> clazz = Class.forName("capers.Utils");
        Method writeContents = clazz.getDeclaredMethod("writeContents", File.class, Object[].class);
        writeContents.setAccessible(true);
        writeContents.invoke(null, file, new Object[]{"hello world"}); // 是覆盖写入
    }

    @Test
    public void test2() {
        // 创建目录
        File file = new File("dummy");
        boolean mkdir = file.mkdir();
    }

    // java 的序列化类
    @Test
    public void test3() throws IOException {
        JavaBean trump = new JavaBean("trump", 18, 5);
        File file = new File("javabean");
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
        stream.writeObject(trump);
        stream.close();
    }

    // 反序列化，从序列化文件中读取并解析为类
    @Test
    public void test4() throws IOException, ClassNotFoundException {
        File file = new File("javabean");
        ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
        JavaBean bean = (JavaBean) stream.readObject();
        stream.close();
        System.out.println(bean);
    }

    // 读取文件
    @Test
    public void test5() throws IOException {
        File file = new File("dummy.txt");
        FileReader fileReader = new FileReader(file);
        BufferedReader inputStream = new BufferedReader(fileReader);
        String line = inputStream.readLine();
        System.out.println(line);
        fileReader.close();
        inputStream.close();
    }

    // 通过流写入文件
    // 如果有多个流，需要先关闭外部流，再关闭内部流
    @Test
    public void test6() throws IOException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        try {
            File file = new File("dummy.txt");
            if (!file.exists()) file.createNewFile();
            // BufferedReader：public String readLine(): 读一行文字。
            fileWriter = new FileWriter(file, true); // append == true 表示追加写入
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.newLine();
            bufferedWriter.write("fucking world");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 注意关闭流的顺序！
            bufferedWriter.close();
            fileWriter.close();
        }

    }

    @Test
    public void test7() throws IOException {
        File file = new File("test-folder/new.txt");
        boolean mkdir = file.mkdir();
        boolean newFile = file.createNewFile();
    }


}
