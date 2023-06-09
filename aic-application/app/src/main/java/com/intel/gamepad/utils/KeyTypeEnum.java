/* Copyright (C) 2021 Intel Corporation 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *   
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.intel.gamepad.utils;


public enum KeyTypeEnum {

    KEYCODE_ESCAPE(111, 1),
    KEYCODE_1(8, 2),
    KEYCODE_2(9, 3),
    KEYCODE_3(10, 4),
    KEYCODE_4(11, 5),
    KEYCODE_5(12, 6),
    KEYCODE_6(13, 7),
    KEYCODE_7(14, 8),
    KEYCODE_8(15, 9),
    KEYCODE_9(16, 10),
    KEYCODE_0(7, 11),
    KEYCODE_MINUS(69, 12),
    KEYCODE_EQUALS(70, 13),
    KEYCODE_DEL(67, 14),
    KEYCODE_TAB(61, 15),
    KEYCODE_Q(45, 16),
    KEYCODE_W(51, 17),
    KEYCODE_E(33, 18),
    KEYCODE_R(46, 19),
    KEYCODE_T(48, 20),
    KEYCODE_Y(53, 21),
    KEYCODE_U(49, 22),
    KEYCODE_I(37, 23),
    KEYCODE_O(43, 24),
    KEYCODE_P(44, 25),
    KEYCODE_LEFT_BRACKET(71, 26),
    KEYCODE_RIGHT_BRACKET(72, 27),
    KEYCODE_ENTER(66, 28),
    KEYCODE_CTRL_LEFT(113, 29),
    KEYCODE_A(29, 30),
    KEYCODE_S(47, 31),
    KEYCODE_D(32, 32),
    KEYCODE_F(34, 33),
    KEYCODE_G(35, 34),
    KEYCODE_H(36, 35),
    KEYCODE_J(38, 36),
    KEYCODE_K(39, 37),
    KEYCODE_L(40, 38),
    KEYCODE_SEMICOLON(74, 39),
    KEYCODE_APOSTROPHE(75, 40),
    KEYCODE_GRAVE(68, 41),
    KEYCODE_SHIFT_LEFT(59, 42),
    KEYCODE_BACKSLASH(73, 43),
    KEYCODE_Z(54, 44),
    KEYCODE_X(52, 45),
    KEYCODE_C(31, 46),
    KEYCODE_V(50, 47),
    KEYCODE_B(30, 48),
    KEYCODE_N(42, 49),
    KEYCODE_M(41, 50),
    KEYCODE_COMMA(55, 51),
    KEYCODE_PERIOD(56, 52),
    KEYCODE_SLASH(76, 53),
    KEYCODE_SHIFT_RIGHT(60, 54),
    KEYCODE_NUMPAD_MULTIPLY(155, 55),
    KEYCODE_ALT_LEFT(57, 56),
    KEYCODE_SPACE(62, 57),
    KEYCODE_CAPS_LOCK(115, 58),
    KEYCODE_F1(131, 59),
    KEYCODE_F2(132, 60),
    KEYCODE_F3(133, 61),
    KEYCODE_F4(134, 62),
    KEYCODE_F5(135, 63),
    KEYCODE_F6(136, 64),
    KEYCODE_F7(137, 65),
    KEYCODE_F8(138, 66),
    KEYCODE_F9(139, 67),
    KEYCODE_F10(140, 68),
    KEYCODE_NUM_LOCK(143, 69),
    KEYCODE_SCROLL_LOCK(116, 70),
    KEYCODE_NUMPAD_7(151, 71),
    KEYCODE_NUMPAD_8(152, 72),
    KEYCODE_NUMPAD_9(153, 73),
    KEYCODE_NUMPAD_SUBTRACT(156, 74),
    KEYCODE_NUMPAD_4(148, 75),
    KEYCODE_NUMPAD_5(149, 76),
    KEYCODE_NUMPAD_6(150, 77),
    KEYCODE_NUMPAD_ADD(157, 78),
    KEYCODE_NUMPAD_1(145, 79),
    KEYCODE_NUMPAD_2(146, 80),
    KEYCODE_NUMPAD_3(147, 81),
    KEYCODE_NUMPAD_0(144, 82),
    KEYCODE_NUMPAD_DOT(158, 83),
    KEYCODE_ZENKAKU_HANKAKU(211, 85),
    KEYCODE_F11(141, 87),
    KEYCODE_F12(142, 88),
    KEYCODE_RO(217, 89),
    KEYCODE_HENKAN(214, 92),
    KEYCODE_KATAKANA_HIRAGANA(215, 93),
    KEYCODE_MUHENKAN(213, 94),
    KEYCODE_NUMPAD_COMMA(159, 95),
    KEYCODE_NUMPAD_ENTER(160, 96),
    KEYCODE_CTRL_RIGHT(114, 97),
    KEYCODE_NUMPAD_DIVIDE(154, 98),
    KEYCODE_SYSRQ(120, 99),
    KEYCODE_ALT_RIGHT(58, 100),
    KEYCODE_MOVE_HOME(122, 102),
    KEYCODE_DPAD_UP(19, 103),
    KEYCODE_PAGE_UP(92, 104),
    KEYCODE_DPAD_LEFT(21, 105),
    KEYCODE_DPAD_RIGHT(22, 106),
    KEYCODE_MOVE_END(123, 107),
    KEYCODE_DPAD_DOWN(20, 108),
    KEYCODE_PAGE_DOWN(93, 109),
    KEYCODE_INSERT(124, 110),
    KEYCODE_FORWARD_DEL(112, 111),
    KEYCODE_VOLUME_MUTE(164, 113),
    KEYCODE_VOLUME_DOWN(25, 114),
    KEYCODE_VOLUME_UP(24, 115),
    KEYCODE_POWER(26, 116),
    KEYCODE_NUMPAD_EQUALS(161, 117),
    KEYCODE_BREAK(121, 119),
    KEYCODE_KANA(218, 122),
    KEYCODE_EISU(212, 123),
    KEYCODE_YEN(216, 124),
    KEYCODE_META_LEFT(117, 125),
    KEYCODE_META_RIGHT(118, 126),
    KEYCODE_MENU(82, 127),
    KEYCODE_MEDIA_STOP(86, 128),
    KEYCODE_COPY(278, 133),
    KEYCODE_PASTE(279, 135),
    KEYCODE_CUT(141, 137),
    KEYCODE_CALCULATOR(210, 140),
    KEYCODE_SLEEP(223, 142),
    KEYCODE_WAKEUP(224, 143),
    KEYCODE_EXPLORER(64, 150),
    KEYCODE_ENVELOPE(65, 155),
    KEYCODE_BOOKMARK(174, 156),
    KEYCODE_BACK(4, 158),
    KEYCODE_FORWARD(125, 159),
    KEYCODE_MEDIA_CLOSE(128, 160),
    KEYCODE_MEDIA_EJECT(129, 161),
    KEYCODE_MEDIA_NEXT(87, 163),
    KEYCODE_MEDIA_PLAY_PAUSE(85, 164),
    KEYCODE_MEDIA_PREVIOUS(88, 165),
    KEYCODE_MEDIA_RECORD(130, 167),
    KEYCODE_MEDIA_REWIND(89, 168),
    KEYCODE_CALL(5, 169),
    KEYCODE_MUSIC(209, 171),
    KEYCODE_HOME(3, 172),
    KEYCODE_REFRESH(285, 173),
    KEYCODE_NUMPAD_LEFT_PAREN(162, 179),
    KEYCODE_NUMPAD_RIGHT_PAREN(163, 180),
    ;

    private final int name;
    private final int value;

    KeyTypeEnum(int name, int value) {
        this.name = name;
        this.value = value;
    }

    public static int findValue(int key) {
        KeyTypeEnum[] values = KeyTypeEnum.values();
        for (KeyTypeEnum value : values) {
            if (value.name == key) {
                return value.value;
            }
        }
        return -1;
    }

    public int getName() {
        return name;
    }

    public int getValue() {
        return value;
    }


}
