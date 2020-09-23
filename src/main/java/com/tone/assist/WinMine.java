package com.tone.assist;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.tone.lib.number.Numbers;
import com.tone.lib.string.Pads;

import javax.swing.*;
import java.awt.*;

import static com.tone.assist.WindowsH.*;

public class WinMine extends JFrame {
    private static final byte MINE_END = 0x10; // 一行雷结束了的结束符
    private static final byte MINE = -113; // 0x8F代表雷
    private static final byte RATIO = 100; // 系统缩放比例
    private int height; // 雷区高度
    private byte[] byteArray; // 雷区列表

    public WinMine() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("扫雷助手");

        JLabel label = new JLabel("雷区：");
        this.getContentPane().add(label, BorderLayout.WEST);
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        this.getContentPane().add(textArea, BorderLayout.CENTER);
        JPanel panel = new JPanel(new GridLayout(6, 1, 2, 5));
        JButton btnRead = new JButton("读取");
        btnRead.addActionListener(event -> {
//            System.out.println("读取中...");
            if (this.getMines() != null) {
                StringBuilder stringBuilder = new StringBuilder("       ");
                for (int i = 0; i < 30; i++) {
                    stringBuilder.append(Pads.leftPad(i + 1, 2)).append("  ");
                    if (i % 3 == 0) {
                        stringBuilder.append(' ');
                    }
                }
                stringBuilder.append('\n');
                for (int i = 0; i < height; i++) {
                    stringBuilder.append(Pads.leftPad(i + 1, 2)).append("   ");
                    for (int j = 0; j < 32; j++) {
                        byte b = byteArray[i * 32 + j];
                        if (b == MINE_END) {
                            break;
                        } else {
                            String str;
                            switch (b) {
                                case MINE: // 0x8F
                                    str = "雷   ";
                                    break;
                                default:
                                    str = "非   ";
                                    break;
                            }
                            stringBuilder.append(str);
                        }
                    }
                    stringBuilder.append('\n');
                }
                textArea.setText(stringBuilder.toString());
            }
        });
        panel.add(btnRead);
        JButton btnAuto = new JButton("自动扫雷");
        btnAuto.addActionListener(event -> {
//            System.out.println("自动扫雷...");
            WinDef.HWND hWnd = this.getMines();
            if (hWnd != null) {
                // 计算坐标位置
                final short baseX = 20;
                final short baseY = 60;
                final short step = 16;
                short[] xyPosition = new short[2];
                WinDef.WPARAM wParam = new WinDef.WPARAM(MK_LBUTTON);
                for (int i = 0; i < height; i++) {
                    // y坐标要在x坐标后边。由于这里使用的是字节的高位对应数字的高位
                    // 发现一个很奇怪的问题，x和y的坐标全部被缩小到了80%。原因未知，所以这边给他放大
                    // 坐标缩小80%的原因找到了，是因为笔记本系统中的缩放与布局调整到了125%
                    xyPosition[1] = (short) ((baseY + step * i) * RATIO / 100);
                    for (int j = 0; j < 32; j++) {
                        byte b = byteArray[i * 32 + j];
                        if (b == MINE_END) {
                            break;
                        } else if (b != MINE){
                            // 不是雷才点击
                            xyPosition[0] = (short) ((baseX + step * j) * RATIO / 100);
                            /*xyPosition[0] = (short)20;
                            xyPosition[1] = (short)60;*/
                            WinDef.LPARAM lParam = new WinDef.LPARAM(Numbers.short2Int(xyPosition));
                            // 这里用PostMessage和SendMessage都可以.只是PostMessage是非阻塞的，而SendMessage是阻塞的
                            User32.INSTANCE.PostMessage(hWnd, WM_LBUTTONDOWN, wParam, lParam);
                            User32.INSTANCE.PostMessage(hWnd, WM_LBUTTONUP, wParam, lParam);
//                            User32.INSTANCE.SendMessage(hWnd, WM_LBUTTONUP, wParam1, lParam);
//                            System.out.println("发送消息 x = " + j + " : " +  xyPosition[0] + ", y = " + i + " : " + xyPosition[1]);
                        }
                    }
                }
            }
        });
        panel.add(btnAuto);
        // 重新开始
        JButton btnReset = new JButton("重置");
        btnReset.addActionListener(event -> {
            this.sendWmCommand(510);
        });
        panel.add(btnReset);
        // 切换各种等级的菜单
        JButton btnLow = new JButton("初级");
        btnLow.addActionListener(event -> {
            this.sendWmCommand(0x209);
        });
        JButton btnMiddle = new JButton("中级");
        btnMiddle.addActionListener(event -> {
            this.sendWmCommand(0x20A);
        });
        JButton btnHigh = new JButton("高级");
        btnHigh.addActionListener(event -> {
            this.sendWmCommand(0x20B);
        });
        panel.add(btnLow);
        panel.add(btnMiddle);
        panel.add(btnHigh);
        this.getContentPane().add(panel, BorderLayout.EAST);
    }

    private void sendWmCommand(int menuId) {
        WinDef.HWND hWnd = this.getWindow();
        if (hWnd != null) {
            WinDef.WPARAM wParam = new WinDef.WPARAM(menuId);
            User32.INSTANCE.PostMessage(hWnd, WM_COMMAND, wParam, null);
        }
    }

    private WinDef.HWND getWindow() {
        // 获取游戏窗口句柄
        WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, "扫雷");
        if (hWnd == null) {
            JOptionPane.showMessageDialog(this, "打开扫雷进程失败");
        }
        return hWnd;
    }

    private WinDef.HWND getMines() {
        WinDef.HWND hWnd = this.getWindow();
        if (hWnd != null) {
            // 通过窗口句柄获取游戏进程ID
            IntByReference processId = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(hWnd, processId);
            // 通过进程ID拿到进程句柄
            WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, processId.getValue());
            // 读取进程的内存信息
            // 读取雷区宽高信息。0x01005334宽    0x01005338高
            Pointer pointer = new Pointer(0x01005334); // 宽高基地址
            Pointer bufPointer = new Memory(8);
            Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, bufPointer, 8, null);
//            int width = bufPointer.getInt(0); // 宽度可以不需要，因为一行读取到0x10就算结束了。这样也就是知道宽度了
            height = bufPointer.getInt(4);
            if (height <= 0 || height > 24) {
                JOptionPane.showMessageDialog(this, "读取宽高信息出错，读取的高度为" + height);
                return null;
            }
            // 读取雷区内存信息
            pointer = new Pointer(0x01005361); // 雷区基地址
            bufPointer = new Memory(height * 32); // 知道高度后，就不要读取24 * 32这么多了
            Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, bufPointer, height * 32, null);
            byteArray = bufPointer.getByteArray(0, height * 32);
        }
        return hWnd;
    }

    public static void main(String[] args) {
        // 显示应用GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // 创建及设置窗口
                WinMine frame = new WinMine();
                frame.setSize(800, 600);
                frame.setLocation(400, 100);
                // 显示窗口
                frame.setVisible(true);
            }
        });
    }
}
