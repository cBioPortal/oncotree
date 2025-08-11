import pandas as pd

file_a = pd.read_excel("reference_oncotree_codes.xlsx")
oncotree_codes_a = file_a.iloc[:, 0].dropna()

file_b = pd.read_excel("staticCrosswalkOncotreeMappings.xlsx")
file_b = file_b.astype(str)
file_b.replace({'': pd.NA}, inplace=True)

oncotree_codes_b = file_b.iloc[:, 0].dropna()
missing_codes = oncotree_codes_a[~oncotree_codes_a.isin(oncotree_codes_b)]

print("The following OncoTree codes from File A are NOT found in File B:")
for code in missing_codes:
    print("-", code)


output_df = missing_codes
output_file_path = "missing_oncotree_codes.xlsx"
output_df.to_excel(output_file_path, index=False)