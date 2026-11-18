# 같은 틱에 방어력 수정자를 더하고 바로 피해를 받는다.
# 피격 직전 갱신이 동작하면 상한이 적용된 방어력으로 계산된다.
# 사용: 서바이벌, 철 갑옷 풀세트, 1급(완료 0~1개)에서 /function fantasyrpg:armorcap/same_tick
# 기대: 받은 피해×1000 = 7200 (피해 7.2). 상한이 늦게 적용되면 2000 (피해 2.0)
function fantasyrpg:armorcap/_before
attribute @s minecraft:generic.armor modifier add fantasyrpg:test_same_tick 10 add_value
damage @s 10 minecraft:mob_attack
attribute @s minecraft:generic.armor modifier remove fantasyrpg:test_same_tick
function fantasyrpg:armorcap/_report
