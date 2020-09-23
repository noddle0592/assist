package com.tone.assist;

/**
 * Winuser.h文件里面定义的按键值
 */
public interface WindowsH {
    int WM_COMMAND = 0x111; // 菜单被按下的消息

    int WM_MOUSEFIRST = 0x0200;
    int WM_MOUSEMOVE = 0x0200;
    int WM_LBUTTONDOWN = 0x0201;
    int WM_LBUTTONUP = 0x0202;
    int WM_LBUTTONDBLCLK = 0x0203;
    int WM_RBUTTONDOWN = 0x0204;
    int WM_RBUTTONUP = 0x0205;
    int WM_RBUTTONDBLCLK = 0x0206;
    int WM_MBUTTONDOWN = 0x0207;
    int WM_MBUTTONUP = 0x0208;
    int WM_MBUTTONDBLCLK = 0x0209;

    int MK_LBUTTON = 1; // 左鼠标键被按下
    int MK_RBUTTON = 2; // 右鼠标键被按下
    int MK_SHIFT = 4; // Shift键被按下
    int MK_CONTROL = 8; // Ctrl键被按下
    int MK_MBUTTON = 16; // 中鼠标键被按下
}
