import streamlit as st
import pandas as pd
import numpy as np
import PIL.Image as Image
import io
import matplotlib.pyplot as plt
from datetime import datetime
import json
import time
import os
from pathlib import Path

# Set up the page
st.set_page_config(
    page_title="WasteWise AI - Segregation System",
    page_icon="♻️",
    layout="wide"
)

# Initialize session state
if 'user_data' not in st.session_state:
    st.session_state.user_data = {
        'points': 100,
        'level': 1,
        'reports': []
    }

if 'community_data' not in st.session_state:
    st.session_state.community_data = {
        'performance': 78,  # percentage
        'rank': 3,
        'total_users': 254
    }

# Simulate ML model
class WasteClassificationModel:
    def __init__(self):
        self.classes = ['Organic', 'Recyclable', 'Hazardous', 'Mixed']
        self.class_colors = {
            'Organic': '#4CAF50',
            'Recyclable': '#2196F3', 
            'Hazardous': '#F44336',
            'Mixed': '#FF9800'
        }
    
    def predict(self, image):
        # Simulate prediction (in a real system, this would be a CNN model)
        np.random.seed(hash(image.tobytes()) % 1000)
        
        # Simulate different predictions based on image characteristics
        if len(image.getbands()) == 1:  # Grayscale
            dominant_class = np.random.choice(['Organic', 'Recyclable'])
        else:
            color_ratio = np.mean(np.array(image)) / 255
            if color_ratio > 0.7:
                dominant_class = 'Recyclable'
            elif color_ratio < 0.3:
                dominant_class = 'Hazardous'
            else:
                dominant_class = np.random.choice(['Organic', 'Mixed'])
        
        # Generate confidence scores
        scores = np.random.dirichlet(np.ones(4), size=1)[0]
        dominant_index = self.classes.index(dominant_class)
        scores[dominant_index] = max(scores) + 0.2  # Boost the dominant class
        scores = scores / scores.sum()  # Re-normalize
        
        return dict(zip(self.classes, scores))

# Create model instance
model = WasteClassificationModel()

# App layout
st.title("♻️ WasteWise AI: Smart Segregation System")
st.markdown("---")

# Sidebar
with st.sidebar:
    st.header("User Profile")
    st.metric("Eco Points", st.session_state.user_data['points'])
    st.metric("Level", st.session_state.user_data['level'])
    st.metric("Community Rank", f"#{st.session_state.community_data['rank']} / {st.session_state.community_data['total_users']}")
    
    st.markdown("---")
    st.subheader("Quick Actions")
    if st.button("🏠 Dashboard"):
        st.session_state.current_page = "dashboard"
    if st.button("📊 Community"):
        st.session_state.current_page = "community"
    if st.button("📸 Classify Waste"):
        st.session_state.current_page = "classify"
    if st.button("🎓 Training"):
        st.session_state.current_page = "training"
    
    st.markdown("---")
    st.info("Earn points by properly segregating your waste and participating in community cleanups!")

# Main content area
tab1, tab2, tab3, tab4 = st.tabs(["Dashboard", "Classify Waste", "Training", "Community"])

with tab1:
    st.header("Personal Dashboard")
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.subheader("Your Segregation Performance")
        performance_data = pd.DataFrame({
            'Week': ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
            'Accuracy': [65, 72, 80, 88]
        })
        st.line_chart(performance_data, x='Week', y='Accuracy')
        
        st.subheader("Recent Reports")
        for i, report in enumerate(st.session_state.user_data['reports'][-3:]):
            st.write(f"{report['date']}: {report['waste_type']} - {report['points']} points")
    
    with col2:
        st.subheader("Waste Distribution")
        waste_data = pd.DataFrame({
            'Type': ['Organic', 'Recyclable', 'Hazardous'],
            'Amount (kg)': [12.5, 8.2, 1.3]
        })
        st.bar_chart(waste_data, x='Type', y='Amount (kg)')
        
        st.subheader("Next Collection")
        st.info("""
        **Organic Waste**: Tomorrow, 9 AM  
        **Recyclables**: Friday, 10 AM  
        **Hazardous**: Next month (1st)
        """)

with tab2:
    st.header("Classify Your Waste")
    
    uploaded_file = st.file_uploader("Upload an image of your waste", type=['jpg', 'jpeg', 'png'])
    
    if uploaded_file is not None:
        image = Image.open(uploaded_file)
        st.image(image, caption="Uploaded Waste Image", use_column_width=True)
        
        if st.button("Analyze Waste"):
            with st.spinner("Analyzing waste composition..."):
                # Simulate processing time
                time.sleep(2)
                
                # Get prediction
                prediction = model.predict(image)
                
                # Display results
                st.subheader("Classification Results")
                
                # Find the dominant class
                dominant_class = max(prediction, key=prediction.get)
                confidence = prediction[dominant_class]
                
                col1, col2 = st.columns(2)
                
                with col1:
                    st.metric("Waste Type", dominant_class, f"{confidence*100:.1f}% confidence")
                    
                    # Show confidence scores
                    st.write("Confidence breakdown:")
                    for waste_type, score in prediction.items():
                        color = model.class_colors[waste_type]
                        st.markdown(f"<span style='color:{color}; font-weight:bold'>{waste_type}: {score*100:.1f}%</span>", 
                                   unsafe_allow_html=True)
                
                with col2:
                    # Show appropriate bin based on classification
                    bin_colors = {
                        'Organic': '#4CAF50',
                        'Recyclable': '#2196F3',
                        'Hazardous': '#F44336',
                        'Mixed': '#FF9800'
                    }
                    
                    st.write("Dispose in:")
                    st.markdown(f"""
                    <div style="background-color: {bin_colors[dominant_class]}; 
                                padding: 20px; 
                                border-radius: 10px; 
                                text-align: center;
                                color: white;
                                font-weight: bold;
                                font-size: 24px;">
                        {dominant_class} Bin
                    </div>
                    """, unsafe_allow_html=True)
                
                # Award points based on classification
                points_earned = 0
                if dominant_class != 'Mixed' and confidence > 0.7:
                    points_earned = 10
                    st.session_state.user_data['points'] += points_earned
                    st.success(f"Good job! You earned {points_earned} points for proper segregation!")
                else:
                    st.warning("Your waste seems to be mixed. Please separate it properly for better recycling.")
                
                # Save report
                report = {
                    'date': datetime.now().strftime("%Y-%m-%d %H:%M"),
                    'waste_type': dominant_class,
                    'confidence': confidence,
                    'points': points_earned
                }
                st.session_state.user_data['reports'].append(report)

with tab3:
    st.header("Waste Segregation Training")
    
    st.subheader("Learn How to Properly Segregate Waste")
    
    training_topics = {
        "Organic Waste": {
            "description": "Biodegradable waste that can be composted",
            "examples": "Food scraps, garden waste, paper towels",
            "color": "#4CAF50"
        },
        "Recyclable Waste": {
            "description": "Materials that can be processed and used again",
            "examples": "Plastic bottles, cardboard, glass containers",
            "color": '#2196F3'
        },
        "Hazardous Waste": {
            "description": "Potentially harmful materials requiring special handling",
            "examples": "Batteries, chemicals, electronic waste",
            "color": '#F44336'
        }
    }
    
    selected_topic = st.selectbox("Select a waste category to learn about:", list(training_topics.keys()))
    
    if selected_topic:
        topic = training_topics[selected_topic]
        st.markdown(f"""
        <div style="background-color: {topic['color']}20; 
                    padding: 20px; 
                    border-radius: 10px;
                    border-left: 5px solid {topic['color']};">
            <h3 style="color: {topic['color']}">{selected_topic}</h3>
            <p><strong>Description:</strong> {topic['description']}</p>
            <p><strong>Examples:</strong> {topic['examples']}</p>
        </div>
        """, unsafe_allow_html=True)
    
    st.subheader("Test Your Knowledge")
    quiz_question = {
        "question": "Which bin should you use for food scraps?",
        "options": ["Organic Bin", "Recyclable Bin", "Hazardous Bin", "Mixed Waste Bin"],
        "answer": "Organic Bin"
    }
    
    st.write(quiz_question["question"])
    selected_answer = st.radio("Select your answer:", quiz_question["options"], index=None)
    
    if selected_answer:
        if selected_answer == quiz_question["answer"]:
            st.success("Correct! Food scraps go in the organic bin where they can be composted.")
        else:
            st.error("Incorrect. Food scraps should go in the organic bin.")

with tab4:
    st.header("Community Engagement")
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.subheader("Community Leaderboard")
        leaderboard = pd.DataFrame({
            'User': ['EcoWarrior42', 'GreenThumb21', 'RecycleMaster', 'You', 'SustainableLife'],
            'Points': [1250, 980, 875, st.session_state.user_data['points'], 620]
        }).sort_values('Points', ascending=False)
        
        st.dataframe(leaderboard, hide_index=True)
        
        st.subheader("Upcoming Events")
        st.info("""
        - **Community Cleanup**: Saturday, 10 AM at Central Park
        - **Composting Workshop**: Next Wednesday, 6 PM at Community Center
        - **E-Waste Collection Drive**: Month end, 9 AM - 2 PM
        """)
    
    with col2:
        st.subheader("Community Performance")
        st.metric("Overall Segregation Accuracy", f"{st.session_state.community_data['performance']}%")
        
        # Community stats
        stats_data = pd.DataFrame({
            'Category': ['Organic', 'Recyclable', 'Hazardous', 'Mixed'],
            'Amount (tons)': [12.3, 8.7, 1.2, 4.5]
        })
        
        fig, ax = plt.subplots()
        ax.pie(stats_data['Amount (tons)'], labels=stats_data['Category'], 
               colors=[model.class_colors[c] for c in stats_data['Category']],
               autopct='%1.1f%%')
        ax.set_title("Community Waste Distribution")
        st.pyplot(fig)
        
        st.subheader("Report Illegal Dumping")
        report_text = st.text_area("Describe the location and situation:")
        report_image = st.file_uploader("Upload evidence photo", type=['jpg', 'jpeg', 'png'], key="report")
        if st.button("Submit Report"):
            st.success("Thank you for your report! The community team will investigate.")

# Footer
st.markdown("---")
st.markdown("""
<style>
.footer {
    position: relative;
    bottom: 0;
    width: 100%;
    text-align: center;
    padding: 10px;
    color: gray;
}
</style>
<div class="footer">
    <p>WasteWise AI ♻️ | Promoting Sustainable Waste Management Practices</p>
</div>
""", unsafe_allow_html=True)