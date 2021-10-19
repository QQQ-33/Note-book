# Git 基本操作
```bash
# 拉取代码
git clone https://github.com/gafish/gafish.github.com.git

# 配置开发者用户名和邮箱<有多种验证用户的方式，还可以配置.ssh>
git config --help
git config user.name gafish
git config user.email gafish@qqqq.com

# 分支操作
git branch
git branch -m # 重命名
git branch -c # 复制分支
git branch -a # 列出所有本地和远程分支
git branch -d # 删除分支

git checkout test # 切换分支
git checkout -b daily/0.0.1 # 从当前分支创建新分支

git merge 
# 快速合并，简单的移动指针到当前 HEAD(顺着一条线能直接到达当前分支)， 用于确定没有冲突或者 hotfix 等情况。
# 正常合并，使用当前两个分支的快照及两个分支的公共祖先快照进行合并。

# 重设分支
git reset --mixed <commit> # 保留文件修改并回退到某个 commit
git reset --soft <commit> # 缓存区和工作区的内容不变，HEAD 指向某个 commit
git reset --hard <commit> # 强制回退到某个 commit， 丢弃所有更改

# 撤销操作
git revert HEAD # 撤销前一次提交
git revert -n HEAD # 撤销多次提交

# 查看当前分支的编辑状态
git status

# 添加文件到缓冲区<commit 之前的操作， IDE 里可以设置自动添加>
git add . # 添加全部
git add test.txt # 添加指定路径
git add -i # 交互式子命令系统
git add -i revert # 从缓冲区移除文件
git add -i patch # 查看当前内容和本地库中的差异，然后决定是否添加到缓冲区

# 本地提交
git commit -m '注释，通常是提交的内容等'

# 远程提交
git push # 提交当前分支
git push origin daily/0.0.1 # 只把某个分支提交到远程

# 拉取代码
git pull # 拉取当前分支
git pull origin daily/0.0.1 # 拉取某个分支

# 提交日志
git log

# 标记， 打了 tag 的内容会独立出来，不能进行更改，可以方便上线。 
git tag v1.0.0 # 轻量标记
git tag -a v1.0.0 -m '注释' # 带注释的 tag
git push origin v1.0.0 # 必须手动提交到远程
git tag -d v1.0.0 # 删除本地 tag
git push origin --delete v1.0.0 # 删除远程 tag， 还可以 git push origin :refs/tags/v1.0.0 ， :refs/tags/ 是固定写法

# 忽略
# .gitignore 文件标注不需要进行版本控制的内容

# 查看差异
git diff
```