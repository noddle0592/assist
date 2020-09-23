package com.tone.assist;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.tone.lib.number.Numbers;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class PlantsVsZombies extends JFrame {
    // 无冷却以及后台运行的几个地址
    private static final int NO_CD = 0x00487296;
    private static final int CHOMPER_NO_CD = 0x00461565;
    private static final int POTATO_NO_CD = 0x0045FE53;
    private static final int BACK_RUN = 0x0054EC05;

    public PlantsVsZombies() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("植物大战僵尸助手");
        this.getContentPane().setLayout(new GridLayout(10, 1));
        this.getContentPane().add(this.newSetPanel(" 阳光 ", 99900, 0x006A9EC0, new int[]{0x768, 0x5560}));
        this.getContentPane().add(this.newSetPanel(" 金币 ", 9000, 0x006A9EC0, new int[]{0x82C, 0x28}));
        JPanel panelChk = new JPanel(new GridLayout(1, 4));
        JCheckBox noCD = new JCheckBox("无需冷却");
        noCD.addActionListener(event -> {
            byte[] bytes;
            if (noCD.isSelected()) {
                // 0x90 0x90 nop nop
                bytes = new byte[]{-112, -112};
            } else {
                // jle short PlantsVs.004872AC
                bytes = new byte[]{0x7E, 0x14};
            }
            this.setAddressMemory(NO_CD, null, bytes);
        });
        panelChk.add(noCD);
        JCheckBox chomperNoCD = new JCheckBox("大嘴花立吞");
        chomperNoCD.addActionListener(event -> {
            byte[] bytes;
            if (chomperNoCD.isSelected()) {
                // 0x90 0x90 nop nop
                bytes = new byte[]{-112, -112};
            } else {
                // jnz short PlantsVs.004615C6
                bytes = new byte[]{0x75, 0x5F};
            }
            this.setAddressMemory(CHOMPER_NO_CD, null, bytes);
        });
        panelChk.add(chomperNoCD);
        JCheckBox potatoNoCD = new JCheckBox("土豆雷立马好");
        potatoNoCD.addActionListener(event -> {
            byte[] bytes;
            if (potatoNoCD.isSelected()) {
                // 0x90 nop
                bytes = new byte[]{-112, -112, -112, -112, -112, -112};
            } else {
                // 0F85 FA010000    jnz PlantsVs.00460053
                bytes = new byte[]{0x0F, -123, -6, 1, 0, 0};
            }
            this.setAddressMemory(POTATO_NO_CD, null, bytes);
        });
        panelChk.add(potatoNoCD);
        JCheckBox backRun = new JCheckBox("后台运行");
        backRun.addActionListener(event -> {
            byte[] bytes;
            if (backRun.isSelected()) {
                // 0x90 0x90 nop nop
                bytes = new byte[]{-112, -112};
            } else {
                // call edx
                bytes = new byte[]{-1, -46};
            }
            this.setAddressMemory(BACK_RUN, null, bytes);
        });
        panelChk.add(backRun);
        this.getContentPane().add(panelChk);
        // 判断以上4个CheckBox的noCD状态
        Function<Pointer, Boolean> isCheckFunction = pointer -> pointer.getByte(0) == -112 && pointer.getByte(1) == -112;
        noCD.setSelected(isCheckFunction.apply(this.getAddressMemory(NO_CD, null, 2)));
        chomperNoCD.setSelected(isCheckFunction.apply(this.getAddressMemory(CHOMPER_NO_CD, null, 2)));
        potatoNoCD.setSelected(isCheckFunction.apply(this.getAddressMemory(POTATO_NO_CD, null, 2)));
        backRun.setSelected(isCheckFunction.apply(this.getAddressMemory(BACK_RUN, null, 2)));
        // 自动放置植物

    }

    private JPanel newSetPanel(String name, int num, int address, int[] offsets) {
        JPanel panelSun = new JPanel(new BorderLayout(3, 1));
        JLabel lblSun = new JLabel(name);
        panelSun.add(lblSun, BorderLayout.WEST);
        JTextField txtSun = new JTextField(String.valueOf(num));
        panelSun.add(txtSun, BorderLayout.CENTER);
        JButton btnSun = new JButton("设置");
        btnSun.addActionListener(event -> {
            int sun;
            try {
                sun = Integer.parseInt(txtSun.getText());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "需要设置的" + name + "必须为数字，请检查");
                return;
            }
            if (sun > 0) {
                this.setAddressMemory(address, offsets, Numbers.int2Byte(sun));
            } else {
                JOptionPane.showMessageDialog(this, "需要设置的" + name + "必须为正数");
            }
        });
        panelSun.add(btnSun, BorderLayout.EAST);
        return panelSun;
    }

    /**
     * 窗口句柄，加快速度用
     */
    private WinDef.HWND hWnd;
    /**
     * 进程句柄，加快速度用
     */
    private WinNT.HANDLE handle;

    private WinDef.HWND getWindow() {
        if (hWnd == null) {
            // 获取游戏窗口句柄
            hWnd = User32.INSTANCE.FindWindow(null, "植物大战僵尸中文版");
            if (hWnd == null) {
                JOptionPane.showMessageDialog(this, "打开植物大战僵尸进程失败");
            }
        }
        return hWnd;
    }

    private WinNT.HANDLE getProcessHandle() {
        if (this.handle == null) {
            WinDef.HWND hWnd = this.getWindow();
            if (hWnd != null) {
                // 通过窗口句柄获取游戏进程ID
                IntByReference processId = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hWnd, processId);
                // 通过进程ID拿到进程句柄
                handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, processId.getValue());
            }
        }
        return this.handle;
    }

    /**
     * 根据基地址和偏移，获取到要访问的最终地址
     * @param address 基地址
     * @param offsets 偏移列表
     * @return 最终数据的存储地址
     */
    private int getFinalAddress(int address, int[] offsets) {
        // 获取需要设置的内存地址信息
        int realAddress = address;
        if (offsets != null && offsets.length > 0) {
            // 获取最终要设置的地址
            for (int i = 0; i < offsets.length; i++) {
                Pointer pointer = new Pointer(realAddress);
                Pointer addrPointer = new Memory(4);
                Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, addrPointer, 4, null);
                realAddress = addrPointer.getInt(0) + offsets[i];
            }
        }
        return realAddress;
    }

    /**
     * 获取远程进程的内存信息
     *
     * @param address   需要设置的内存基地址
     * @param offsets   基地址的偏移地址数据，可支持多个偏移地址
     * @param size      需要读取的数据大小，以字节为单位
     * @return 读取到的内存指针
     */
    private Pointer getAddressMemory(int address, int[] offsets, int size) {
        Pointer pointer = new Pointer(this.getFinalAddress(address, offsets));
        Pointer bufPointer = new Memory(size);
        Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, bufPointer, size, null);
        return bufPointer;
    }

    /**
     * 设置远程进程的内存信息
     *
     * @param address   需要设置的内存基地址
     * @param offsets   基地址的偏移地址数据，可支持多个偏移地址
     * @param byteArray 需要设置的数据
     * @return 是否成功
     */
    private boolean setAddressMemory(int address, int[] offsets, byte[] byteArray) {
        if (byteArray != null && byteArray.length > 0) {
            WinNT.HANDLE handle = this.getProcessHandle();
            if (handle != null) {
                // 设置进程的内存信息
                Pointer pointer = new Pointer(this.getFinalAddress(address, offsets));
                Pointer bufPointer = new Memory(byteArray.length);
                bufPointer.write(0, byteArray, 0, byteArray.length);
                Kernel32.INSTANCE.WriteProcessMemory(handle, pointer, bufPointer, byteArray.length, null);
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // 显示应用GUI
        SwingUtilities.invokeLater(() -> {
            // 创建及设置窗口
            PlantsVsZombies frame = new PlantsVsZombies();
            frame.setSize(800, 600);
            frame.setLocation(300, 100);
            // 显示窗口
            frame.setVisible(true);
        });
    }
}
