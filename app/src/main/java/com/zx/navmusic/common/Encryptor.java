package com.zx.navmusic.common;

public class Encryptor {

    private static final String KEY = "95wwwHiFiNicom27";

    public static void main(String[] args) {
        String data = "Ypn4ARo65f0TNUO6IseHoTLbQ6YFnfPV7DQbMfx7P1fey2lJHAoyKrMNDLwobnvyu0uocNgQN8IRL6JGxV4klQ"; // 要加密的输入数据
        String encryptedParam = generateParam(data);
        System.out.println(encryptedParam);
    }

    // 生成加密参数
    public static String generateParam(String data) {
        StringBuilder outText = new StringBuilder();

        for (int i = 0, j = 0; i < data.length(); i++, j++) {
            if (j == KEY.length()) j = 0;
            outText.append((char) (data.charAt(i) ^ KEY.charAt(j))); // 异或加密
        }

        return base32Encode(outText.toString());
    }

    // Base32 编码
    public static String base32Encode(String str) {
        String base32chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder bits = new StringBuilder();
        StringBuilder base32 = new StringBuilder();

        // 转换每个字符为二进制并拼接
        for (int i = 0; i < str.length(); i++) {
            String bit = String.format("%8s", Integer.toBinaryString(str.charAt(i))).replace(' ', '0');
            bits.append(bit);
        }

        // 确保位数为5的倍数
        while (bits.length() % 5 != 0) {
            bits.append("0");
        }

        // 将二进制数据以5位为单位进行Base32编码
        for (int i = 0; i < bits.length(); i += 5) {
            String chunk = bits.substring(i, i + 5);
            int index = Integer.parseInt(chunk, 2);
            base32.append(base32chars.charAt(index));
        }

        // 填充使长度为8的倍数
        while (base32.length() % 8 != 0) {
            base32.append("=");
        }

        // 替换填充符号
        return base32.toString().replace("=", "HiFiNiYINYUECICHANG");
    }
}

