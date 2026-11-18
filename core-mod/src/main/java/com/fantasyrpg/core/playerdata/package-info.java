/**
 * 플레이어 RPG 상태 저장 (기획 v0.4 2·7장, 프로젝트 문서 claude/플레이어성장_설계.md).
 *
 * <p>지금 구현: NeoForge 데이터 첨부 {@code fantasyrpg:player}에 직업·레벨·경험치·사용 SP·시작 SP·완료 보너스 기록
 * ({@link com.fantasyrpg.core.playerdata.RpgPlayerData}), 순수 계산({@link com.fantasyrpg.core.playerdata.ProgressionMath}),
 * 서버 처리({@link com.fantasyrpg.core.playerdata.RpgProgress}).
 * 나중에: 노드 랭크, 장착 슬롯(액티브 4, 궁극기 1, 활성 마스터리 1), 빌드 프리셋(최대 3), 클라이언트 동기화.
 */
package com.fantasyrpg.core.playerdata;
