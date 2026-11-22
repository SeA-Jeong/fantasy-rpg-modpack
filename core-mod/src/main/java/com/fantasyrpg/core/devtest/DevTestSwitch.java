package com.fantasyrpg.core.devtest;

/**
 * 개발자 전용 테스트 장비를 켤지 정하는 규칙 (순수 로직).
 * <ol>
 *   <li>시스템 속성 {@code -Dfantasyrpg.devtest=true|false} 가 있으면 그것을 따른다 (가장 우선).</li>
 *   <li>없으면 {@code config/fantasyrpg-devtest/ENABLE} 파일이 있으면 켠다 (테스트 팩에 들어 있음).</li>
 *   <li>둘 다 없으면 개발 환경(Gradle runClient·runServer)에서만 켠다. 일반 배포(production)에서는 꺼진다.</li>
 * </ol>
 * 아이템 등록은 게임 시작 때 한 번이므로, 바꾸면 게임을 다시 켜야 한다.
 * 클라이언트와 서버의 설정이 다르면 아이템 목록이 달라 접속이 거부된다(한 컴퓨터 싱글플레이는 같은 설정).
 */
public final class DevTestSwitch {
    private DevTestSwitch() {
    }

    public static boolean enabled(boolean production, String systemProperty, boolean enableFileExists) {
        if (systemProperty != null && !systemProperty.isBlank()) return Boolean.parseBoolean(systemProperty.trim());
        if (enableFileExists) return true;
        return !production;
    }
}
