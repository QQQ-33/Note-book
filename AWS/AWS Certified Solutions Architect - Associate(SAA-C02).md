https://zhenye-na.github.io/aws-certs-cheatsheet
# AWS Region & Availability Zone
### Region
> A region is a geographical area. Each Region consists of 2 or more Availability Zone.

> region 是一个地理上的区域，每个region包含 2至多个 AZ

### Availability Zone
> An Availability Zone may be several Data Centers. If they (Data Centers) are close to each other, they may be counted as one Availability Zone.

> AZ 是一个或多个数据中心，如果数据中心距离较近会算作一个 AZ

### Edge Locations
> Edge Locations are endpoints for AWS which are used for caching content. Typically, this consists of CloudFront, Amazon’s Content Delivery Network.

![region](./assert/region-az-edgelocation.png)

### Exam Tips
1. A **Region** is a physical location in the world, which consists of two or more Availability Zones.
2. An **Availability Zone** is one or more discrete Data Centers, each with redundant power, networking, and connectivity, housed in a separate facility.
3. **Edge Locations** are the endpoints for AWS which are used for caching content. Typically, this consists of CloudFront, Amazon’s Content Delivery Network.

# Identity Access Management (IAM)
- IAM allows you to manage users, groups, roles, permissions (level of access) to the AWS console
IAM has a global view, not need to choose a Region

### IAM Features
IAM offers the following features:
1. Centralized control of your AWS account
2. Shared Access to your AWS account
3. Granular Permissions
4. Identity Federation (Active Directory, Github …)
5. Multi-Factor Authentication
6. Provide temporary access for users/devices and services where necessary
7. Allows you to set up your password rotation policy
8. integrates with many different AWS services
9. Support PCI DSS Compliance

### Terminology of IAM
> Users(a physicial person)<br>
> Groups(grant to users) <br>
> Roles (grant to a mechine or a AWS service)<br>
> Policies (a JSON formatted file, call Policy Document)

### IAM - Advanced
**AWS STS - Security Token Service**
- Allows granting limited and temporary access to AWS resources
- Token is valid for up to one hour (must be refreshed)
```
AssumeRole
AssumeRoleWithSAML
AssumeRoleWithWebIdentity
GetSessionToken
```
**Using STS to Assume a Role**
- Define an IAM Role within your account or cross-account
- Define which principals can access this IAM Role
- Use AWS STS to retrieve credentials and impersonate the IAM Role you have access to (AssumeRole API)
- Temporary credentials can be valid between 15 minutes to 1 hour
> 创建 IAM role并设置 role 的权限， 设置谁能通过 STS 获取 role，通过STS 相关 API 获取 role， 每个 STS 凭证可以保持最多 1 小时就要刷新

![sts](./assert/sts-assumerole.png)

### Directory Services
- AWS Managed Microsoft AD
    - Create your AD in AWS, manage users locally, supports MFA
    - Establish trust connections with your on-premise AD
- AD Connector
    - Directory Gateway (proxy) to redirect to on-premise AD
    - Users are managed on the on-premise AD
- Simple AD
    - AD-compatible managed directory on AWS
    - Cannot be joined with on-premise AD

### AWS Organizations
- Global service
- Allows managing multiple AWS accounts
- The main account is the master account (cannot change this)
- Other accounts are member accounts
- Member accounts can only be part of one organization
- Consolidated billing across all accounts - one payment method
- API is available to automate AWS account creation

**Organization Units (OU)**
![OU](./assert/AWS_Organizations-1024x467.png)
### Service Control Policies (SCP)
- SCP contains Allowlist or denylist IAM actions and can be applied at the Root, OU, or Account level
- SCP is applied to all the Users and Roles of the Account, including Root
The SCP does not affect service-linked roles
service-linked roles enable other AWS services to integrate with AWS Organizations and cannot be restricted by SCPs
- SCP must have an explicit Allow rule (since it does not allow anything by default)
> SCP 基于服务的控制策略，SCP 包含一个可以(或禁止)操作的服务和资源列表，可以分配给各个层级的 account。<br>
> SCP 不能影响与服务关联的 role(service-linked roles)<br>
> SCP 必须指定允许什么，因为默认它什么都不允许做

### IAM Roles vs Resource-Based Policies
Attach a policy to a resource versus attaching of a using a role as a proxy (STS)
When you assume a role (user, application, or service), you give up your original permissions and take the permissions assigned to the role
When using a resource-based policy, the principal does not have to give up his permissions
> 使用 STS 时，相当于放弃原有的权限，使用的是 assume 的 role<br> 
> 使用 resource-based policy 时，原有的权限依然存在

### IAM - Policy Evaluation Logic
As soon as there is an explicit deny, the result will be denied
> 有明确的拒绝时，以拒绝为准
```json 
"Statement": [
    {
        "Action": "sqs:*",
        "Effect": "Deny",
        "Resource": "*"
    },
    {
        "Action": [
            "sqs:DeleteQueue"
        ],
        "Effect": "Allow",
        "Resource": "*"
    }
]
// 这里 sqs:DeleteQueue 将不会生效，因为 sqs:* 被 Deny 了。
```
### AWS Resource Access Manager (RAM)
- Share AWS resources that you own with other AWS accounts
- Share with any account or within your Organization
- VPC Subnets, AWS Transit Gateway, Route53 Resolver Rules, License Manager Configurations

### AWS Single Sign-On (SSO)
- Centrally manage Single Sign-On to access multiple accounts and 3rd party business applications
- Integrated with AWS Organizations
- Support SAML 2.0 markup
- Integration with on-premise Active Directory
- Centralized permission management
- Centralized auditing with CloudTrail
### SSO vs AssumeRoleWithSAML
![OU](./assert/sso-assumerolewithsaml.png)

# Elastic Compute Cloud (EC2)
Amazon Elastic Compute Cloud is a web service that provides resizable compute capacity in the cloud.

EC2 Pricing Model / Type
- On Demand: pay a fixed rate by the hour, no commitment
- Reserved: provide a capacity reservation, minimum 1 year
  1. Standard Reserved Instance
  2. Convertible Reserved Instance
  3. Scheduled Reserved Instance
- Spot: based on “bid”, like stock, on-demand
- Dedicated Instances
- Dedicated Hosts: physical EC2 Instance

> - 按需付费，适合短时间使用
>   - 最高的费用，但是无需预付款
>   - 适用与无法预测合适会发生且不能中断的工作
> - 预付费(至少一年)：
>   - 标准的预付费
>       - 比按需付费节省 75%
>       - 需要指定实例的类型，
>       - 适合长时间使用的实例，比如 database
>   - 有可能变更实例类型的预付费
>       - 比按需付费节省 50%
>       - 可以改变实例类型
>   - 周期性的预留(比如每周的周末使用，其余时间不使用)
>       - 适合只在窗口时间运行的任务
> - 现货
>   - 用户设定一个可接受的最大使用费用，如果当前的实例费用小于这个费用则用户可以使用实例，一旦现货的价格超过用户设定的价格，AWS会通知用户，用户有权决定按照新价格继续使用还是停止使用。
>   - 比按需付费便宜 90%
>   - 适合运行不怕失败的任务，batch job， data analysis等
> - 实体的EC2
>   - 价格会贵一些
>   - 有完全的控制权，使用自己提供的软件 licens 
>   - 适合需要强力监管的工作

### Security Groups
“Network & Security” -> “Security Groups”

When you create a new inbound rule, you will also create a new outbound rule
- Security Groups are stateful
- Network Access Control List is stateless
- All inbounds traffic is blocked by default, all outbound traffic is allowed
- Changes to Security Groups take effect immediately
- You can have any number of EC2 instances with a Security Group
- You cannot block network access in the security groups, instead we should use “Network Access Control Lists” (VPC Section)
- Allow Rule (OK), Deny Rules (X)
> Security Group 类似 firewall 定义了入站出站规则，默认出站是 all traffic，入站是 none <br>
> SG的变更将立即生效，SG只能规定什么能进入，不能规定什么不能进入

Security Group controls the inbound and outbound traffic of EC2

Security Group acts as the role like:
- "firewall" (connection-wise) on EC2 Instance
- Access to Ports
- IP Ranges
- Inbound/Outbound Network

Referencing other Security Groups
![SG](./assert/sg-referencing.png)

### private & public IP
> 默认 EC2会有一个 private ip (ipv4)，如果想通过 internet访问EC2则需要一个 public IP<br>
> 可以通过将EC2 放入具有 IGW(Internet Gateway) 的subnet使其获取一个 public IP 以及一个 public DNS。但是每次 stop/start EC2 时，public IP会动态变化<br>
> 如果想使用固定的 public IP，可以将 EC2 绑定一个 Elastic IP(EIP)。

### EC2 User Data
> 创建 EC2 实例时，可以执行一些初始化操作，这些命令只在创建实例时执行一次。

### EC2 Instance Types - Main Ones
https://www.ec2instances.info
- R	High RAM usage
- C	High CPU usage
- M	(Medium) balanced usage application
- I	High I/O
- G	High GPU usage
- T2/T3 - burstable	burstable instance with a limit threshold
- T2/T3 - unlimited	unlimited burstable amount
> R 内存优化，适合缓存<br>
> C CPU优化，适合做计算或者DB<br>
> M 平衡行，适合做 WEB<br>
> I I/O优化，适合做 DB<br>
> G GPU 优化，适合ML或者图形工作<br>
> burstable 使用 CPU 积分在某些运算量激增的时间爆发大量性能，其余时间计算能力维持在基准线之下<br>
> unlimited 不限制爆发计算的时间，但是需要额外付费

### AMI
an image to use to create our instances
> AMI 可以共享给其他 account<br>
> 共享之后你依然是 AMI 的 owner<br>
> 要copy 一个AMI 那么这个 AMI 所有者的账号必须给你这个AMI保存的 EBS或者 S3 的read 权限<br>
> 不能 copy 加密的 AMI，除非加密的 key 和 snapshot 也共享给你，你可以用你自己的 key 重新加密 snapshot 然后做成新的 AMI<br>
> 不能 copy 使用了计费代码的 AMI， 你可以先 lanuch 一个 EC2，然后用这个 EC2 做一个 AMI<br>

### EC2 Placement Groups
安置策略，如何放置 EC2 实例
> - Cluster 集群式部署，实例都在一个机架一个AZ，具有很低的延迟，但是风险很高，一个机架 fail 所有instance全部宕机
> - Spread 分布式部署，实例分布在不同AZ，分散了风险，适合要求高可用的项目，每个 Group 做多支持 7个 instance
> - Partition 分区部署，类似分布式部署，在分布式的基础上按照机架进行分区，相同分区的instance在同一个机架上，适合 Hadoop，Kafka 等要求高可用的分布式系统，同一个 AZ 可达100 instance， 一个Group最多支持7个Partition

1. A clustered placement group cannot span multiple AZs, while a spread placement and a partitioned group can
2. The name you specify for a placement group must be unique within your AWS account
3. Only certain types of instances can be launched in a placement group, these types contain Compute Optimized, GPU, Memory Optimized, Storage Optimized, etc...
4. AWS recommend homogeneous instances within clustered placement groups
5. You cannot merge placement groups
6. You cannot move an existing instance into a placement group. You can create an AMI from your existing instance, and then launch a new instance from the AMI into a placement group

### Elastic Network Interfaces (ENI)
VPC中的一块虚拟网卡
> 创建EC2时默认带一个 primary ENI， 可以创建其他 ENI 并 attach到 EC2
> 每个ENI可以包含如下属性：
> 1. 一个 private IP
> 2. 一个 EIP
> 3. 若干个 SG
> 4. 一个 MAC 地址
> ENI 需要绑定一个AZ

### Hibernate EC2
![Hibernate](./assert/hibernation-flow.png)
> Stop 时使保存EC2 的运行状态(类似休眠)，这些state写入EBS的文件，ESB卷必须是加密的，且有足够的空间保存EC2 State

使用的场景：
- 长时间运行的流程
- 需要很长时间初始化
- 需要保存内存状态

### High Availability and Scalability - ELB & ASG
- 垂直扩展 scale up and scale down
- 水平扩展 scale in and scale out

**Horizontal Scalability**
- increase the number of instances/systems, distributed system

**Vertical Scalability**
- increase the size of the instance, like from t2.micro to t2.large, etc…

**HA (High Availability)**
1. HA usually goes hand-in-hand with Horizontal Scalability
2. HA means running your application/system in at least 2 data centers, to be specific, 2 AZs
3. HA can be both active or passive
    - active: Horizontal Scalability
    - passive: RDS Multi-AZ

There are two methods of Horizontal Scaling:
- Auto Scaling Group - ASG
- Load Balancer

### Load Balancing (Elastic Load Balancer - ELB)
Load Balancers are servers that forward internet traffic to multiple servers (EC2) downstream

But, why would we use a load balancer, anyway?
1. Spread load across multiple downstream instances
2. Expose a single point of access (DNS) to your application
3. Seamlessly handle failures of downstream instances, through health checks
4. Do regular health checks to your instances
5. Provide SSL Termination (HTTPS) for your website
6. Enforce stickiness with cookies
7. HA across AZs
8. Separate public traffic from private traffic
> 1. 分流
> 2. 对外暴露一个 DNS
> 3. 故障引流
> 4. 周期性的故障检查
> 5. 提供 HTTPS 访问
> 6. 增强 cookie 粘性
> 7. 跨AZ的HA
> 8. 分离公共和私有流量

There are three types of Load Balancers on AWS

- Application Load Balancer (ALB) 7层的层面
    - HTTP, HTTPS, WebSocket
    - supports SSL
- Network Load Balancer (NLB) 4层的层面
    - TCP, TLS (secure TCP) & UDP
    - supports SSL
- Classic Load Balancer (CLB)
    - HTTP, HTTPS, TCP
    - DO NOT support SSL


**Classic Load Balancer (CLB)**

This is just legacy Load Balancers

**Network Load Balancer (NLB)**

It balances TCP (layer 4) traffic

> 转发 TCP/UDP 流量

Forward TCP & UDP traffic to your instances. Network Load Balancer can handle millions of requests per second while maintaining ultra-low latencies, ~100ms, where 400ms for ALB

> 每秒处理数百万的请求，延迟低至 100ms，ALB需要 400ms

NLB has one static IP per AZ and supports assigning Elastic IP

> NLB 在每个 AZ 有一个静态 IP，支持挂载 EIP

**Application Load Balancer (ALB)**

It balances HTTP / HTTPS traffic, you can also create
> 转发 HTTP/HTTPS 流量 (layer 7)
- advanced request routing
- sending specific requests to specific web servers

> 可以通过设置 rule 来指定请求转发至下游的哪个 target Group<br>
> 比如根据请求的路径转发流量

**Target Groups**
> Each target group is used to route requests to one or more registered targets.

where targets can be:

- EC2 Instances (can be managed by an ASG) - HTTP
- ECS tasks (Elastic Container Service) - HTTP
- Lambda functions - HTTP request if translated into a JSON event
- IP Address - must be private IPs

ALB can also route to multiple target groups, also, Health checks are at the target group level
> ALB 可以转发流量至多个 targetGroup， healthCheck 是 targetGroup 级别的

it has a fixed hostname, (xxx.<region>.elb.amazonaws.com, etc..)
> ALB 有一个域名

the application servers don't see the IP of the client directly, if you wanna see, then:
the true IP of the client is inserted in the header X-Forwarded-For
we can also get the Port (X-Forwarded-Port) and proto (X-Forwarded-Proto)
> 应用端不能直接获取客户端的 IP，客户端的IP放在请求的 header 的 (X-Forwarded-For) 以及 (X-Forwarded-Proto) 中

**Sticky Sessions**

Sticky Session allows you to bind a user’s session to a specific EC2 Instance. This ensures that all requests from the user during the session are sent to the same instance

you can enable the “sticky session” for Application Load Balancer, but the traffic will be sent at the “Target Group” level, rather than Individual EC2 Instance
> 粘性 session 将 client 的流量导向同一个 targetGroup。CLB和ALB都支持，对于 ALB 需要在 targetGroup 开启 sticky session，并设置持续时间，它是利用 cookie 来实现的。

**Cross-zone Load Balancing**

- With Cross Zone Load Balancing each load balancer instance distributes evenly across all registered instances in all AZ
- Otherwise, each load balancer node distributes requests evenly across the registered instances in its AZ only

> 有了跨AZ的 load Balance， 每个 balancer 将转发流量到分布在每个AZ的 EC2 实例<br>
> 不使用跨AZ的 LB， 每个 balancer 将转发流量到当前AZ的 instance

Cross-Zone in 3 types of Load Balancers

- Application Load Balancer
  - Always on (cannot be disabled)
  - No charges for inter AZ data
- Network Load Balancer
  - Disabled by default
  - You pay charges for inter AZ data is enabled
- Classic Load Balancer
  - Disabled by default
  - No charges for inter AZ data is enabled

**Elastic Load Balancer - SSL Certificates**
> 要使用安全的链接 SSL/TSL 需要在LB上添加 HTTPS/TSL Listener，同时指定一个 CA证书<br>
> CLB 不能同时使用多个证书<br>
> ALB 和 NLB 可以为每个 targetGroup指定一个证书，他们使用 SNI 技术来区分转发流量时使用哪个证书加密。

### Auto Scaling Group (ASG)
the goal for an ASG is to:

1. Scale in or scale out to match an increased load or decreased load
2. Ensure minimum / maximum number of running instances
3. automatically register new instances to a load balancer

> 1. 为了应对负载的增加/减少，而水平扩展/缩减
> 2. 确保运行的实例数在 min/max 之间
> 3. 自动将新 lanuch 的实例注册到 LB

![asg](./assert/asg.png)

**Scaling Policies**
- Target Tracking Scaling
    - Most simple and easy to set-up
    - Example I want the average ASG CPU to stay at around 40%
- Simple / Step Scaling
    - When some metrics is triggered, do something
- Scheduled Actions
    - eg. increase the min capacity to 10 at 5 pm.

### Elastic Block Store (EBS) EBS
> - 类似一个 USB，可以快速的 attach 给一个实例，
> - 只能在同AZ内相互传递，不能跨AZ
> - 需要为所有容量付费
> - 4种型号：
>   - GP2(SSD): 普通的SSD，兼顾价格和性能，适合多样性的工作，100-3000 IOPS，最高 16000IOPS， 1GB-16TB
>   - IO1(SSD): 高性能SSD，适合重要的任务，低延迟，高吞吐量，100 - 32000 IOPS，适合大型数据库，4GB-16TB
>   - ST1(HDD): 便宜的HDD，为了频繁访问加强过的HDD，适合想用低价格获取高速读取性能，适合大数据量，数据仓库，日志处理,500GB-16TB，最大500IOPS，最大500MB/s的吞吐量，
>   - SC1(HDD): 最便宜的HDD，适合不经常访问的数据，最大250MB/s的吞吐量，
> - 共同特征是 Size(容量) Throughput(吞吐量，每秒读写数据量) I/OPS(I/O pre Sec，每秒访问次数)
> - 只有SSD可以作为 EC2 的 boot volumes

**EBS Snapshots**
> - 只能做增量复制
> - Snapshots 保存在S3上，但是我们看不见
> - 可以不在 detach volume时做Snapshot，但是推荐先 detach
> - 最多 100000 个Snapshots
> - 可以跨AZ复制，可以用于制作AMI
> - EBS从 Snapshots中恢复数据有个预热的过程
> - Snapshots可以被Amazon Data Lifecycle Manager自动使用(自动备份)

**Encryption**
> 加密EBS
> - EBS 内保存的数据是加密的
> - 数据的访问过程也是加密的
> - 相关的 Snapshots 也是加密的
> - 加密/解密的过程我们无需做任何操作
> - 加密会有一个较小的延迟
> - 使用KMS AES-256 加密
> - copy Snapshots 时可以选择加密
> - 加密的 Snapshots 的 volumns 也是加密的

1. Volumes exist on EBS, think of EBS as Virtual Hard Disk
2. Snapshots are on S3, think of Snapshot as photo of disk
3. Snapshots are the point in time copies of volumes
4. EBS Snapshots are incremental, only the blocks that have changed since your last snapshot are moved to S3
5. To create a snapshot for Amazon EBS Volumes that serve as root devices, you should stop the instance before taking the snapshot
6. You can create AMIs from Volumes and Snapshots
7. You can change EBS Volume sizes on the fly, including changing the size and storage type
8. Volume will ALWAYS be in the same AZ as the EC2 Instance, BUT you can copy snapshots across AZ or Region
9. EBS Backup will utilize IO so you should not enable it while handling a lot of traffic
10. Recommend - detach the EBS volume to do the backup, but not a must
11. EBS volumes restored by snapshots need to be pre-warmed (using fio or dd command to read the entire volume)
12. snapshots can be automated using "Amazon Data Lifecycle Manager"

**Instance Store**
Local EC2 Instance Store is a physical disk attached to the physical server where your EC2 is

it has very high IOPS, but the size of it cannot be increased and the data will be lost if hardware fails to happen
> Instance Store 是挂载在EC2上的实体硬盘，具有非常高的IOPS，但是不能增加容量，并且重启EC2时，数据会丢失。<br>
> 只有某些机型具有 Instance Store

**EBS RAID**
EBS is replicated within an AZ so it is already redundant storage. But if you want to increase the IOPS more or you want to mirror your EBS volumes, then you need to mount volumes in parallel in RAID settings. (RAID is possible as long as your OS supports it)
> EBS 在同AZ中默认时冗余存在的，但如果想增加 IOPS，那么需要在RAID中设置并行挂载(如果EC2的系统支持的话)

Normal RAID options:

- RAID 0
- RAID 1
- RAID 5 - not recommended for EBS
- RAID 6 - not recommended for EBS

RAID 0 - increasing performance
- Combining 2 or more volumes and getting the total disk space and I/O
- But one disk fails, then all the data is failed

Use cases:
    - application needs a lot of IOPS and doesn’t need fault-tolerance
    - a database that has replication already built-in
Using this, we can have a very big disk with a lot of IOPSCombining 2 or more volumes and getting the total disk space and I/O
But one disk fails, then all the data is failed
> 将多个EBS卷合并为一个，提供更高的吞吐，但是提高了数据丢失的风险，一旦一个EBS失效，那么所有EBS将全部失效，数据将全部丢失。

RAID 1 - increase fault tolerance

RAID 1 is to mirror a volume to another, which means if one disk fails, then our logical volume is still working (since there is our mirroring one)

Use case:
    - application that needs to increase volume fault tolerance
    - application that needs service disks
> 增加容错性，不会改变总容量和总吞吐量，会增加一点延迟

### EFS (Elastic File System)
![asg](./assert/overview-flow.png)
EFS is a managed NFS (network file system) that can be mounted on many EC2, EFS can work with EC2 instances in multi-AZ.

EFS is a High Available, Scalable, and expensive service