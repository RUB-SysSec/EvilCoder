cp gcc_cmd_transformer.py ./original/
cd original
#make --dry-run > dry_run.txt
perl -p -e 's/\\\n//' dry_run.txt > concat_lines.txt
grep "^gcc " concat_lines.txt > proc_dry_run.sh
sed -i 's/^gcc/python gcc_cmd_transformer.py/' proc_dry_run.sh
bash proc_dry_run.sh > proc_run.sh
bash proc_run.sh
cd ..

