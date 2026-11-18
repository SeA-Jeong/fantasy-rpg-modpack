# (내부용) 피격 전 체력을 1000배 정수로 저장
scoreboard objectives add frpg_test dummy
execute store result score #before frpg_test run data get entity @s Health 1000
