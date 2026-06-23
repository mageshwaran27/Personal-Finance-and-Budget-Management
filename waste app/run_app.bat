@echo off
echo Installing required packages...
pip install -r requirements.txt

echo Starting WasteWise AI Application...
streamlit run wastewise_ai.py
pause