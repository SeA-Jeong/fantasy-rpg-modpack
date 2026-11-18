# (내부용) 피격 후 체력과 받은 피해를 채팅에 보여 준다. 값은 1000배 정수 (예: 7200 = 피해 7.2)
execute store result score #after frpg_test run data get entity @s Health 1000
scoreboard players operation #taken frpg_test = #before frpg_test
scoreboard players operation #taken frpg_test -= #after frpg_test
tellraw @s ["",{"text":"[방어 시험] ","color":"gold"},{"text":"체력×1000 "},{"score":{"name":"#before","objective":"frpg_test"}},{"text":" → "},{"score":{"name":"#after","objective":"frpg_test"}},{"text":", 받은 피해×1000 = "},{"score":{"name":"#taken","objective":"frpg_test"},"color":"yellow","bold":true},{"text":"  (7200이면 피해 7.2, 끝자리 1 차이는 반올림)","color":"gray"}]
