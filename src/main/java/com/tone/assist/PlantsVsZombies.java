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

public class PlantsVsZombies extends JFrame {

    public PlantsVsZombies() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("植物大战僵尸助手");
        this.getContentPane().setLayout(new GridLayout(10, 1));
        this.getContentPane().add(this.newSetPanel("阳光", 99900, 0x006A9EC0, new int[]{0x768, 0x5560}));
        this.getContentPane().add(this.newSetPanel("金币", 9000, 0x006A9EC0, new int[]{0x82C, 0x28}));
        JPanel panelChk = new JPanel(new GridLayout(1, 3));
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
            this.setAddressMemory(0x00487296, null, bytes);
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
            this.setAddressMemory(0x00461565, null, bytes);
        });
        panelChk.add(chomperNoCD);
        JCheckBox backRun = new JCheckBox("后台运行");
        backRun.addActionListener(event -> {
            /*byte[] bytes;
            if (backRun.isSelected()) {
                // 0x90 0x90 nop nop
                bytes = new byte[]{-112, -112};
            } else {
                // jle 004872AC
                bytes = new byte[]{0x7E, 0x14};
            }
            this.setAddressMemory(0x00487296, null, bytes);*/
        });
        panelChk.add(backRun);
        this.getContentPane().add(panelChk);
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

    private WinDef.HWND getWindow() {
        // 获取游戏窗口句柄
        WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, "植物大战僵尸中文版");
        if (hWnd == null) {
            JOptionPane.showMessageDialog(this, "打开植物大战僵尸进程失败");
        }
        return hWnd;
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
            WinDef.HWND hWnd = this.getWindow();
            if (hWnd != null) {
                // 通过窗口句柄获取游戏进程ID
                IntByReference processId = new IntByReference();
                User32.INSTANCE.GetWindowThreadProcessId(hWnd, processId);
                // 通过进程ID拿到进程句柄
                WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_ALL_ACCESS, false, processId.getValue());
                // 获取需要设置的内存地址信息
                int realAddress = address;
                if (offsets != null && offsets.length > 0) {
                    // 获取最终要设置的地址
                    for (int i = 0; i < offsets.length; i++) {
                        Pointer pointer = new Pointer(realAddress);
                        Pointer addrPointer = new Memory(4);
                        Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, addrPointer, byteArray.length, null);
                        realAddress = addrPointer.getInt(0) + offsets[i];
                    }
                }
                // 设置进程的内存信息
                Pointer pointer = new Pointer(realAddress);
                Pointer bufPointer = new Memory(byteArray.length);
                bufPointer.write(0, byteArray, 0, byteArray.length);
                Kernel32.INSTANCE.WriteProcessMemory(handle, pointer, bufPointer, byteArray.length, null);
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // 显示应用 GUI
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
