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

![region](file:/C:/Users/qk965/Desktop/Tom/AWS/assert/region-az-edgelocation.png)

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

![sts](file:/C:/Users/qk965/Desktop/Tom/AWS/assert/sts-assumerole.png)

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
![OU](file:/C:/Users/qk965/Desktop/Tom/AWS/assert/AWS_Organizations-1024x467.png)
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
![OU](file:/C:/Users/qk965/Desktop/Tom/AWS/assert/sso-assumerolewithsaml.png)

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
![SG](file:/C:/Users/qk965/Desktop/Tom/AWS/assert/sg-referencing.png)

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