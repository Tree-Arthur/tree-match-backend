// 查询队伍和已加入队伍成员的信息
select *
from team t
         left join user_team ut on t.id = ut.teamId
         left join user u on ut.userId = u.id;


SELECT id,name,description,maxNum,expireTime,userId,
       status,password,createTime,updateTime,isDelete
FROM team
WHERE isDelete = 0
  AND (id = ? AND (name LIKE ? OR description LIKE ?) AND name LIKE ? AND description LIKE ? AND maxNum = ? AND
       userId = ? AND status = ? AND (expireTime > ? OR expireTime IS NULL))