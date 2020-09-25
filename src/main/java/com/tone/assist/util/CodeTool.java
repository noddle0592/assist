package com.tone.assist.util;

import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * 读取nasm或者yasm编译器生成的二进制代码，转换成byte数组的工具类
 * 编译的代码必须为32位。在代码第一行增加伪指令bits 32即可
 * 编译命令为nasm.exe -f bin test.asm -o test.o或者yasm.exe -f bin test.asm -o test.o
 * 另外，汇编指令写完之后，记得加一个ret
 * @author zlf
 */
public class CodeTool {
    private static final String file = "D:/nasm-2.15.05/test.o";
    private byte[] code;

    @Before
    public void init() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        // 由于汇编代码基本都比较短，生成的文件也小，所以整个读取
        code = new byte[fileInputStream.available()];
        fileInputStream.read(code);
        fileInputStream.close();
    }

    /**
     * 打印asm对应的二进制字节码
     * 用于直接拷贝到代码中使用
     */
    @Test
    public void printAsmBytes() {
        StringBuilder stringBuilder = new StringBuilder("byte[] code = new byte[]{ ");
        for (byte b : code) {
            stringBuilder.append(b).append(", ");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 2).append('}');
        System.out.println(stringBuilder);
    }

    /**
     * 通过特征码，获取对应的index位置
     * index对应的是特征码的最后一位
     * 用于设置代码中参数的时候方便找到偏移量
     * 可以先通过ndisasm -b 32 hello.o查看特征码
     */
    @Test
    public void getIndex() {
        // 取x坐标，即column
//        byte[] sign = new byte[] {0x6A, 0x01};
        // 取y坐标，即row
        byte[] sign = new byte[] {-72, 0x03};
        int signIdx = 0;
        for (int i = 0; i < code.length; i++) {
            if (code[i] == sign[signIdx]) {
                signIdx++;
                if (signIdx >= sign.length) {
                    System.out.println("sign is " + sign[sign.length - 1] + " and index is " + i);
                    break;
                }
            }
        }
    }
}
