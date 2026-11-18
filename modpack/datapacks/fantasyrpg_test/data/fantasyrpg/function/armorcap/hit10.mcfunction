# 피해 10(몹 공격 유형)을 받고 전후 체력과 받은 피해를 채팅에 보여 준다. 서바이벌에서 사용.
# 함수 안 명령의 결과는 채팅에 나오지 않으므로 tellraw 로 직접 보여 준다.
function fantasyrpg:armorcap/_before
damage @s 10 minecraft:mob_attack
function fantasyrpg:armorcap/_report
