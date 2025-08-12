import pandas as pd
import os

file_a = pd.read_excel("reference_oncotree_codes.xlsx")
oncotree_codes_a = file_a.iloc[:, 0].dropna()

while True:
    file_b = input("Enter name of the file in an Excel format ('.xlsx')")
    if '.xlsx' not in file_b:
        print("You forgot to add '.xlsx'. Please try again and add the required '.xlsx'.")
    elif '.xlsx' in file_b:
        if os.path.exists(file_b):
            file_b = pd.read_excel(file_b)
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
        else:
            print("This file does not exist in your folders. Try transferring the excel file into the same folder as where this code is in")
