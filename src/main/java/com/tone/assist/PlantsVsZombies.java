package com.tone.assist;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
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
    /**
     * 放置植物的二进制代码
     */
    private static final byte[] PUT_PLANT_CODE = new byte[]{ 80, 83, 106, -1, 106, 2, 106, 1, -95, -64, -98, 106, 0, -117,
            -128, 104, 7, 0, 0, 80, -72, 3, 0, 0, 0, -69, 32, -47, 64, 0, -1, -45, 91, 88, -61 };
    /**
     * 植物码代码索引
     */
    private static final byte PLANT_INDEX = 5;
    /**
     * x坐标代码索引
     */
    private static final byte X_COLUMN_INDEX = 7;
    /**
     * y坐标代码索引
     */
    private static final byte Y_ROW_INDEX = 21;
    /**
     * 最多9列
     */
    private static final byte MAX_X_COLUMN = 9;
    /**
     * 最多6行
     */
    private static final byte MAX_Y_ROW = 6;
    /**
     * 窗口句柄，加快速度用
     */
    private WinDef.HWND hWnd;
    /**
     * 进程句柄，加快速度用
     */
    private WinNT.HANDLE handle;
    /**
     * 远程执行代码对应的地址
     */
    private Pointer codeAddress;

    public PlantsVsZombies() throws HeadlessException {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("植物大战僵尸助手");
        this.getContentPane().setLayout(new GridLayout(7, 1,  0, 10));
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
        if (this.getWindow() != null) {
            // 判断以上4个CheckBox的noCD状态
            Function<Pointer, Boolean> isCheckFunction = pointer -> pointer.getByte(0) == -112 && pointer.getByte(1) == -112;
            noCD.setSelected(isCheckFunction.apply(this.getAddressMemory(NO_CD, null, 2)));
            chomperNoCD.setSelected(isCheckFunction.apply(this.getAddressMemory(CHOMPER_NO_CD, null, 2)));
            potatoNoCD.setSelected(isCheckFunction.apply(this.getAddressMemory(POTATO_NO_CD, null, 2)));
            backRun.setSelected(isCheckFunction.apply(this.getAddressMemory(BACK_RUN, null, 2)));
        }
        // 自动放置植物
        this.getContentPane().add(this.newPlantPanel(Type.COLUMN));
        this.getContentPane().add(this.newPlantPanel(Type.ROW));
        this.getContentPane().add(this.newPlantPanel(Type.ALL));
    }

    /**
     * 新建设置阳光或者金币的面板
     * @return 阳光或金币面板
     */
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

    private JPanel newPlantPanel(Type type) {
        JPanel panel = new JPanel(new GridLayout(1, 0, 20, 0));
        String name;
        switch (type) {
            case COLUMN:
                name = "纵向放置";
                break;
            case ROW:
                name = "横向放置";
                break;
            case ALL:
            default:
                name = "全部";
                break;
        }
        JLabel label = new JLabel(name);
        panel.add(label);
        JComboBox<Byte> idxComboBox = new JComboBox<>();
        if (type != Type.ALL) {
            if (type == Type.COLUMN) {
                // 固定最多一次放1列9行
                for (byte i = 0; i < MAX_X_COLUMN; i++) {
                    idxComboBox.addItem(i);
                }
            } if (type == Type.ROW) {
                // 可能有5行1列，或者6行1列
                for (byte i = 0; i < MAX_Y_ROW; i++) {
                    idxComboBox.addItem(i);
                }
            }
            panel.add(idxComboBox);
        }
        JComboBox<Plant> comboBox = new JComboBox<>();
        comboBox.addItem(new Plant(0, "豌豆射手"));
        comboBox.addItem(new Plant(1, "向日葵"));
        comboBox.addItem(new Plant(2, "樱桃炸弹"));
        comboBox.addItem(new Plant(3, "坚果墙"));
        comboBox.addItem(new Plant(4, "土豆雷"));
        comboBox.addItem(new Plant(5, "寒冰射手"));
        comboBox.addItem(new Plant(6, "大嘴花"));
        comboBox.addItem(new Plant(7, "双发射手"));
        comboBox.addItem(new Plant(8, "小喷菇"));
        comboBox.addItem(new Plant(9, "阳光菇"));
        comboBox.addItem(new Plant(10, "大喷菇"));
        comboBox.addItem(new Plant(11, "墓碑吞噬者"));
        comboBox.addItem(new Plant(12, "魅惑菇"));
        comboBox.addItem(new Plant(13, "胆小菇"));
        comboBox.addItem(new Plant(14, "寒冰菇"));
        comboBox.addItem(new Plant(15, "毁灭菇"));
        comboBox.addItem(new Plant(16, "睡莲"));
        comboBox.addItem(new Plant(17, "倭瓜"));
        comboBox.addItem(new Plant(18, "三线射手"));
        comboBox.addItem(new Plant(19, "缠绕海草"));
        comboBox.addItem(new Plant(20, "火爆辣椒"));
        comboBox.addItem(new Plant(21, "地刺"));
        comboBox.addItem(new Plant(22, "火炬树桩"));
        comboBox.addItem(new Plant(23, "高坚果"));
        comboBox.addItem(new Plant(21, "海蘑菇"));
        comboBox.addItem(new Plant(25, "路灯花"));
        comboBox.addItem(new Plant(26, "仙人掌"));
        comboBox.addItem(new Plant(27, "三叶草"));
        comboBox.addItem(new Plant(28, "裂荚射手"));
        comboBox.addItem(new Plant(29, "杨桃"));
        comboBox.addItem(new Plant(30, "南瓜头"));
        comboBox.addItem(new Plant(31, "磁力菇"));
        comboBox.addItem(new Plant(32, "卷心菜投手"));
        comboBox.addItem(new Plant(33, "花盆"));
        comboBox.addItem(new Plant(34, "玉米投手"));
        comboBox.addItem(new Plant(35, "咖啡豆"));
        comboBox.addItem(new Plant(36, "大蒜"));
        comboBox.addItem(new Plant(37, "叶子保护伞"));
        comboBox.addItem(new Plant(38, "金盏花"));
        comboBox.addItem(new Plant(39, "西瓜投手"));
        comboBox.addItem(new Plant(40, "机枪射手"));
        comboBox.addItem(new Plant(41, "双子向日葵"));
        comboBox.addItem(new Plant(42, "犹豫喷菇"));
        comboBox.addItem(new Plant(43, "香蒲"));
        comboBox.addItem(new Plant(44, "冰西瓜"));
        comboBox.addItem(new Plant(45, "吸金磁"));
        comboBox.addItem(new Plant(46, "地刺王"));
        comboBox.addItem(new Plant(47, "玉米加农炮"));
        panel.add(comboBox);
        JButton button = new JButton("放置");
        button.addActionListener(event -> {
            byte index = idxComboBox.getSelectedItem() != null ? (Byte) idxComboBox.getSelectedItem() : 0;
            byte code = ((Plant)comboBox.getSelectedItem()).getCode();
            this.remoteExecute(type, index, code);
        });
        panel.add(button);
        return panel;
    }

    private static class Plant {
        private final byte code;
        private final String name;

        public Plant(int code, String name) {
            this.code = (byte) code;
            this.name = name;
        }

        public byte getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private enum Type {
        /**
         * 全部都放置
         */
        ALL,
        /**
         * 按行放，横向放置
         */
        ROW,
        /**
         * 按列放，纵向放置，最为常见
         */
        COLUMN
    }

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
        if (handle == null) {
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

    private void cleanResource() {
        // 清除分配的内存空间
        if (codeAddress != null) {
            Kernel32.INSTANCE.VirtualFreeEx(handle, codeAddress, new BaseTSD.SIZE_T(PUT_PLANT_CODE.length), WinNT.MEM_DECOMMIT);
            codeAddress = null;
        }
        if (handle != null) {
            Kernel32.INSTANCE.CloseHandle(handle);
            handle = null;
        }
        if (hWnd != null) {
            User32.INSTANCE.CloseWindow(hWnd);
            hWnd = null;
        }
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
                if (Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, addrPointer, 4, null)) {
                    realAddress = addrPointer.getInt(0) + offsets[i];
                } else {
                    // 用户可能重新打开游戏了，清除资源，让下次重新获取
                    this.cleanResource();
                    JOptionPane.showMessageDialog(this, "读取植物大战僵尸进程信息失败，请重新尝试");
                    return 0;
                }
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
        WinNT.HANDLE handle = this.getProcessHandle();
        if (handle != null) {
            int realAddress = this.getFinalAddress(address, offsets);
            if (realAddress > 0) {
                Pointer pointer = new Pointer(realAddress);
                Pointer bufPointer = new Memory(size);
                if (Kernel32.INSTANCE.ReadProcessMemory(handle, pointer, bufPointer, size, null)) {
                    return bufPointer;
                } else {
                    // 用户可能重新打开游戏了，清除资源，让下次重新获取
                    this.cleanResource();
                    JOptionPane.showMessageDialog(this, "读取植物大战僵尸进程信息失败，请重新尝试");
                }
            }
        }
        return new Memory(size);
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
                int realAddress = this.getFinalAddress(address, offsets);
                if (realAddress > 0) {
                    Pointer pointer = new Pointer(realAddress);
                    Pointer bufPointer = new Memory(byteArray.length);
                    bufPointer.write(0, byteArray, 0, byteArray.length);
                    if (Kernel32.INSTANCE.WriteProcessMemory(handle, pointer, bufPointer, byteArray.length, null)) {
                        return true;
                    } else {
                        // 用户可能重新打开游戏了，清除资源，让下次重新获取
                        this.cleanResource();
                        JOptionPane.showMessageDialog(this, "写入植物大战僵尸进程信息失败，请重新尝试");
                    }
                }
            }
        }
        return false;
    }

    private Pointer getCodeAddress() {
        if (codeAddress == null) {
            // 打开目标进程
            WinNT.HANDLE handle = this.getProcessHandle();
            if (handle != null) {
                // 在目标进程分配内存空间
                // VirtualAllocEx分配内存的最小单位是一个内存页，小于的时候会round为一个内存页的大小，这个值在windows上就是4k。
                codeAddress = Kernel32.INSTANCE.VirtualAllocEx(handle, null, new BaseTSD.SIZE_T(PUT_PLANT_CODE.length), WinNT.MEM_COMMIT, WinNT.PAGE_EXECUTE_READWRITE);
                Pointer codePointer = new Memory(PUT_PLANT_CODE.length);
                // 最后一个call中必须要用near absolute call方式
                codePointer.write(0, PUT_PLANT_CODE, 0, PUT_PLANT_CODE.length);
                // 往分配的内存空间中写入代码
                if (Kernel32.INSTANCE.WriteProcessMemory(handle, codeAddress, codePointer, PUT_PLANT_CODE.length, null)) {
                    // 程序退出时，需要删除分配的空间
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            Kernel32.INSTANCE.VirtualFreeEx(handle, codeAddress, new BaseTSD.SIZE_T(PUT_PLANT_CODE.length), WinNT.MEM_DECOMMIT);
                        }
                    });
                    return codeAddress;
                } else {
                    this.cleanResource();
                }
            }
        }
        return codeAddress;
    }

    /**
     * 调用远程进程，执行目标代码
     *
     * @param type  行列，或者全部设置
     * @param index 如果是行列，指定第几行或者第几列
     * @param code  植物码
     * @return 是否成功
     */
    private boolean remoteExecute(Type type, byte index, byte code) {
        Pointer codeAddress = this.getCodeAddress();
        if (codeAddress != null) {
            // 设置植物码
            Pointer plantPointer = codeAddress.share(PLANT_INDEX);
            Pointer positionPointer = new Memory(1);
            positionPointer.setByte(0, code);
            if (Kernel32.INSTANCE.WriteProcessMemory(handle, plantPointer, positionPointer, 1, null)) {
                // 设置坐标
                Pointer xPointer = codeAddress.share(X_COLUMN_INDEX);
                Pointer yPointer = codeAddress.share(Y_ROW_INDEX);
                switch (type) {
                    case COLUMN:
                        // x坐标仅设置一次，y坐标多次设置
                        positionPointer.setByte(0, index);
                        this.forCodeExecute(xPointer, yPointer, MAX_Y_ROW, positionPointer);
                        break;
                    case ROW:
                        // y坐标仅设置一次，x坐标多次设置
                        positionPointer.setByte(0, index);
                        this.forCodeExecute(yPointer, xPointer, MAX_X_COLUMN, positionPointer);
                        break;
                    case ALL:
                    default:
                        // x坐标多次设置，y坐标多次设置
                        for (byte i = 0; i < MAX_X_COLUMN; i++) {
                            positionPointer.setByte(0, i);
                            this.forCodeExecute(xPointer, yPointer, MAX_Y_ROW, positionPointer);
                        }
                        break;
                }
                return true;
            } else {
                JOptionPane.showMessageDialog(this, "写入植物大战僵尸进程信息失败，请重新尝试");
                this.cleanResource();
            }
        }
        return false;
    }

    private void forCodeExecute(Pointer oncePointer, Pointer forPointer, byte count, Pointer positionPointer) {
        if (Kernel32.INSTANCE.WriteProcessMemory(handle, oncePointer, positionPointer, 1, null)) {
            for (byte i = 0; i < count; i++) {
                positionPointer.setByte(0, i);
                if (Kernel32.INSTANCE.WriteProcessMemory(handle, forPointer, positionPointer, 1, null)) {
                    this.codeExecute();
                }
            }
        }
    }

    private boolean codeExecute() {
        // 执行目标进程中指定地址的代码
        // 权限不足常见的是非管理员启动的进程需要调用管理员启动的进程。这时候就需要使用到windows提权的api，在Winbase.h中的OpenProcessToken相关函数。得用c++，Java不行
        WinNT.HANDLE threadHandle = Kernel32.INSTANCE.CreateRemoteThread(handle, null, 0, codeAddress, null, 0, null);
        if (threadHandle != null) {
            // 等待目标执行完成，最多等3秒
            Kernel32.INSTANCE.WaitForSingleObject(threadHandle, 3000);
            Kernel32.INSTANCE.CloseHandle(threadHandle);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        // 显示应用GUI
        SwingUtilities.invokeLater(() -> {
            // 创建及设置窗口
            PlantsVsZombies frame = new PlantsVsZombies();
            frame.setSize(800, 500);
            frame.setLocation(400, 200);
            // 显示窗口
            frame.setVisible(true);
        });
    }
}
