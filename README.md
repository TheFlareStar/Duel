# Duel - 角斗场插件

## 简略介绍
设置为角斗场所在的世界后，玩家在这个世界内击杀其他玩家之后会回满血量(后期可能会改成自定义)
连续击杀5人获得力量I药水效果10秒以及粒子效果，粒子效果可在配置文件选择是否开启。

## 指令 & 权限
- /duel sk 获取标准装备 (注意，是会直接覆盖背包内容的) | 相关权限 duel.use
- /duel kb 查看排行榜 | 相关权限 duel.use
- /duel kset [玩家] [数量] 设置玩家的击杀数 | 相关权限 duel.admin
- /duel reload 重载配置文件 | 相关权限 duel.admin
- 拥有 duel.admin 权限的玩家可在角斗场build为false的情况下建筑
建议将权限 duel.use 给予玩家
 
## 配置文件
```
#角斗场所在的世界
world: world
#角斗场世界的统一重生点
spawn: 124,96,24
#角斗场是否可以建筑
build: false
#是否启用粒子效果(连续击杀5人的奖励)
effect: true
#标准装备配置
standardkit:
  #0为空 1为皮革 2为铁 3为钻石
  armor:
    a: 2
    b: 0
    c: 1
    d: 0
  #0为空 1为木剑 2为石剑 3为铁剑 4为钻石剑
  sword: 2
```
