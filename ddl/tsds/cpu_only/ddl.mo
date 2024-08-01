CREATE TABLE IF NOT EXISTS cpu (
    tags varchar(50), -- 固定值，取值cpu，表示为cpu的时序数据
    id varchar(50), -- 15位cpu id，每一位取值范围：0-9
    created_at varchar(50), -- 这列为时间列，也可以选择datetime这种时间类型
    usage_user varchar(50), -- 用户使用时间比例，取值范围：0~100
    usage_system varchar(50), -- 系统使用时间比例，取值范围：0~100
    usage_idle float,  -- CPU空闲时间比例，取值范围：0~100
    usage_nice float,  -- CPU以nice优先级（取值范围：-20~19）运行的进程用户态时间，取值范围：0~100
    usage_iowait float, -- CPU等待I/O操作的时间比例，取值范围：0~100
    usage_irq float,  -- 硬件中断CPU的时间比例，取值范围：0~100
    usage_softirq float,  -- 软件中断CPU的时间比例，取值范围：0~100
    usage_steal float,  -- CPU被其他虚拟机(VM)或物理服务器占用的时间比例，取值范围：0~100
    usage_guest float, -- CPU运行虚拟处理器时CPU花费的时间比例，取值范围：0~100
    usage_guest_nice float -- CPU运行带有nice优先级的虚拟CPU所花费的时间比例，取值范围：0~100
);
DROP TABLE IF EXISTS tags;
CREATE TABLE IF NOT EXISTS tags (
    id varchar(50), -- 实例的id，比如cpu的id，要在事实表中能找得到的
    tags varchar(50), -- tags类型，这里主要为cpu，也可以加点其它的，比如枚举: cpu, memory, disk, network
    hostname varchar(50),  -- 主机名，格式为host_[digit]
    region varchar(50),  -- 区域，格式是[region/country]-[direction]-[digit]，根据几个枚举值来组合即可。 国家: us, uk, de, fr, ru, cn, jp, kr, sg, my, nz    方位: east, south, west, north  
    datacenter varchar(50),  -- 数据中心，是region后面加一个字母即可，一个region可以对应几个数据中心，比如a, b, c
    rack varchar(50),  -- 机架编号，看起来是个大于0的整型数字即可
    os varchar(50),  -- 操作系统名称和版本，枚举几个值即可: CentOS_6, CentOS_7, CentOS_8, Ubuntu_20, Ubuntu_21, Ubuntu_22, Debian_9, Ubuntu_10, Ubuntu_11, Ubuntu_12, MacOS_13, MacOS_14, Windows_XP, Windows_7, Windows_Vista, Windows_8, Windows_10, Windows_11, OpenEuler_20, Kylin_10, KylinSec_3, UOS_20, OpenCloudOS_8, OpenCloudOS_9, TencentOS_2, TencentOS_3
    arch varchar(50),  -- cpu架构，枚举：x86, x64, arm64, aarch64
    team varchar(50),  -- 团队，看起来是老美的城市简称，我们可以搞几个国内的城市，参考后续附表
    service varchar(50), -- 服务，看起来是个大于0的整型数字即可
    service_version varchar(50), -- 服务版本，看起来是个大于0的整型数字即可
    service_environment varchar(50) -- 环境用途，枚举: development, test, staging, production
);
