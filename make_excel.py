# /usr/bin/python3
# -*- coding: utf-8 -*-
import os
import re
import pandas as pd

directory_path = "./issue_"

def extrace_one_task(task_directory_path) -> pd.DataFrame:
    df1 = extract_one_configure_running(os.path.join(task_directory_path, "before"))
    df2 = extract_one_configure_running(os.path.join(task_directory_path, "after"))

    df_combined = pd.concat([df1, df2], axis=1)

    # %%
    # 重命名列，以便区分两个DataFrame
    df_combined.columns = [
        "QPS_before",
        "RT_MAX_before",
        "RT_MIN_before",
        "QPS_after",
        "RT_MAX_after",
        "RT_MIN_after",
    ]

    # 计算QPS的性能波动百分比
    df_combined["QPS_Change_Percent"] = (
        (df_combined["QPS_after"]) / df_combined["QPS_before"]
    )

    return df_combined

def extract_one_configure_running(config_directory_path) -> pd.DataFrame:
    """
    Extracts and combines log data from all subdirectories within the given directory path.

    Args:
        config_directory_path (str): The path to the directory containing subdirectories with log files.

    Returns:
        pd.DataFrame: A combined DataFrame with QPS, RT_MAX, and RT_MIN columns.
    """
    all_dfs = []

    # 扫描指定目录下的所有文件夹
    for root, dirs, _ in os.walk(config_directory_path):
        for dir_name in dirs:
            log_path = os.path.join(root, dir_name, "time.log")
            if os.path.exists(log_path):
                log_df = extract_one_log(log_path, dir_name)
                all_dfs.append(log_df)

    # 合并所有 DataFrame
    if all_dfs:
        combined_df = pd.concat(all_dfs)

        # 指定的排序顺序
        order = [
            "select_1",
            "select_star",
            "select_where",
            "insert_with_empty_no_index",
            "insert_with_100w_no_index",
            "insert_with_empty_indexes",
            "insert_with_100w_indexes",
            "update",
            "delete",
        ]
        # 获取现有的 index
        existing_indices = combined_df.index.tolist()
        # 按照指定顺序排序
        sorted_indices = sorted(
            existing_indices, key=lambda x: order.index(x) if x in order else len(order)
        )
        combined_df = combined_df.loc[sorted_indices]

        return combined_df
    else:
        return pd.DataFrame(columns=["QPS", "RT_MAX", "RT_MIN"])


def extract_one_log(file_path, dir_name) -> pd.DataFrame:
    """
    Extracts log data from a given log file.

    Args:
        file_path (str): The path to the log file.

    Returns:
        pd.DataFrame: A DataFrame with QPS, RT_MAX, and RT_MIN columns.
    """
    with open(file_path, "r") as file:
        content = file.read()

    # 正则表达式匹配相关部分
    pattern = re.compile(
        r"\[(.*?)\]\nSTART : .*\nEND : .*\nVUSER : \d+\nTPS : \d+\nQPS : (\d+)\nSUCCESS : \d+\nERROR : \d+\nRT_MAX : (\d+)\nRT_MIN : (\d+)\nRT_AVG : .*\nSUC_RATE : .*\nEXP_RATE : .*\nRESULT : .*"
    )
    match = pattern.search(content)

    if match:
        action = match.group(1)
        qps = int(match.group(2))
        rt_max = int(match.group(3))
        rt_min = int(match.group(4))

        parts = dir_name.split("_", 3)
        if len(parts) > 3:
            action = "_".join(parts[3:])            

        # 创建 DataFrame 并将 Action 设为索引
        log_df = pd.DataFrame(
            [[qps, rt_max, rt_min]], columns=["QPS", "RT_MAX", "RT_MIN"], index=[action]
        )
        return log_df
    else:
        return pd.DataFrame(columns=["QPS", "RT_MAX", "RT_MIN"])

#%%
combined_df = extrace_one_task(directory_path)
# 获取最后一个文件夹的名字作为后缀
suffix = os.path.basename(os.path.normpath(directory_path))
file_name = f"result_{suffix}.md"
combined_df.to_markdown(file_name)