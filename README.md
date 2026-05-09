# *Problem Statement*

At the Technical University of Munich (TUM), Bachelor's and Master's students must write a final thesis at the end of the course in order to graduate. A thesis topic can be realized in three ways:

- Students get accepted to take on an open thesis;
- Students come up with their own idea for a thesis;
- Students partner up with industry for a thesis;

The common denominator for all these options is that a TUM chair must supervise the thesis, which implies that the specific thesis **must** be aligned with the interest of the specific chair, as the chair not only grades the work, but also provides an advisor (usually PhD candidate at the chair) to assist the student. 

Coming up with an idea for a thesis or partnering up with industry are by far the most unlikely scenarios, especially when it comes to a chair agreeing to supervise it. Therefore, applying for an open thesis is the path the majority of the students follow.

Essentially, PhD candidates at TUM's chairs are naturally taking on a very complex and/or vast research topic. For this reason, many thesis proposals are connected to ongoing research projects at TUM chairs. Researchers often publish thesis topics to **involve students** in specific subproblems, implementations, experiments, or literature studies related to their research area.

In order to find helpers, PhD candidates post about  "open thesis" topics on **their chair's websites**.  This creates an **information gap** that the proposed platform aims to address.
- Chair's websites do not have to follow a specific structure $\rightarrow$ harder to find where to look;
- Varying levels of description about the open theses' topic;
- Chair's websites are not in evidence $\rightarrow$ interesting topics can be missed/overlooked;
- Even with dedication, navigating through all possible chair's websites and finding where open theses are listed is massively time consuming and frustrating;

### Platform's main functionality

In order to **centralize** and **organize** information, the new online platform encapsulates information about open theses **scraped** from multiple chairs within **one database**.

Users who access the web-platform have access to all latest open theses, as they are routinely scraped by the platform's microservices. 

The platform offers **two** different methods of search:
- Traditional filtering:
		$\rightarrow$ Degree Type
		$\rightarrow$ Research Area
		$\rightarrow$ Keywords
- Usage of natural language to describe the type of thesis they look for.

### Intended Users

TUM **students** currently in the last stage of their **Bachelor's or Master's** who are looking for an open thesis proposal which fits their interests.

### GenAI Integration

The application revolves around GenAI's capabilities. It is integrated into the platform in the following manners:
- After raw HTML is scraped from chair websites, AI is responsible to generate structured output in JSON with all fields necessary in order for the theses to be added to the database;
- AI will generate an overview of the thesis. The overview will be a summary of theses with extensive descriptions and will enhance theses that provide little description;
#### Vector Database
An additional AI related feature offered by the application is **natural-language semantic search**. Users can describe their thesis interests in natural language, which is transformed into an **embedding vector** and compared against thesis embeddings in a vector database. The closest matches are then used to retrieve the corresponding thesis entries from the relational database.

### Scenarios of Application Usage

At the Technical University of Munich (TUM), Bachelor's and Master's students must write a final thesis at the end of their degree program in order to graduate. A thesis topic can usually be realized in three ways:

- Students apply for an already published open thesis;
- Students propose their own thesis idea;
- Students cooperate with an industry partner on a thesis topic.

The common requirement for all these options is that the thesis must be supervised by a TUM chair. This means that the topic has to be aligned with the research interests and expertise of a specific chair, since the chair is responsible not only for grading the thesis, but also for providing an advisor, usually a PhD candidate or researcher at the chair, to guide the student throughout the process.

Coming up with an individual thesis idea or partnering with industry is possible, but in practice these are often more difficult paths, especially because a chair must still agree to supervise the topic. Therefore, applying for an already published open thesis is the path followed by many students.

PhD candidates and researchers at TUM chairs usually work on complex and broad research topics. For this reason, many open thesis proposals are connected to ongoing research projects at the chairs. These topics allow students to contribute to specific subproblems, implementations, experiments, evaluations, or literature studies that are relevant to the chair's research area.

To find suitable students, chairs publish open thesis topics on their own websites. However, since these thesis postings are scattered across many different chair websites, an information gap is created. This is the gap that the proposed platform aims to address.

The current situation presents several problems:

- Chair websites do not follow a standardized structure, which makes it difficult to know where to look;
- The level of detail provided for thesis descriptions varies significantly;
- Many chair websites are not highly visible, meaning that interesting topics can easily be missed or overlooked;
- Even for motivated students, manually navigating through many chair websites and searching for open thesis listings is time-consuming and frustrating.

### Platform's Main Functionality

In order to centralize and organize information, the proposed online platform collects information about open theses from multiple TUM chairs and stores it in a single database.

Users who access the web platform can browse the latest available open thesis proposals, which are routinely collected by the platform's scraping microservices. Instead of visiting many different chair websites individually, students can use one centralized platform to discover thesis opportunities across different research areas.

The platform offers two main methods of search:

- Traditional filtering:
  - Degree type;
  - Research area;
  - Keywords;
  - Chair or department;
  - Availability status.

- Natural-language semantic search:
  - Users can describe the type of thesis they are interested in using natural language;
  - The system compares this description with the available thesis entries and returns the most relevant results.

### Intended Users

The intended users are TUM students who are currently in the final stage of their Bachelor's or Master's degree and are looking for an open thesis proposal that matches their interests, skills, and academic goals.

The platform is especially useful for students who do not yet know which chair they want to work with, or who want to explore thesis opportunities across multiple research areas without manually checking each chair website.

### GenAI Integration

The application integrates GenAI and AI-related techniques in multiple parts of the system.

First, after raw HTML content is scraped from chair websites, AI is used to extract structured information from unstructured or semi-structured web pages. The goal is to transform inconsistent website content into a standardized JSON format containing the fields required to store the thesis entry in the database.

Second, AI can generate a concise overview of each thesis. For thesis postings with long descriptions, this overview acts as a summary. For thesis postings with very limited descriptions, the overview can help clarify the topic by extracting and rephrasing the available information in a more understandable way.

Third, the platform provides natural-language semantic search. Users can describe their thesis interests in their own words, and the system transforms this query into an embedding vector. This vector is then compared against thesis embeddings stored in a vector database. The closest matches are used to retrieve the corresponding thesis entries from the relational database.

This feature is AI-powered rather than purely rule-based. It allows users to search based on meaning instead of only exact keywords. For example, a student interested in "machine learning for medical images" may still find relevant theses that use terms such as "computer vision", "neural networks", "healthcare data", or "image classification", even if the exact words from the query do not appear in the thesis title.

Optionally, GenAI can also be used to generate a short explanation for why a thesis matches the user's query. This would make the recommendation process more transparent and easier to understand.

### Scenarios of Application Usage

#### Scenario 1: Traditional Filtering

A Master's student in Informatics is looking for a thesis related to cybersecurity. The student opens the platform and uses the traditional filtering system. They select "Master's Thesis" as the degree type and "Cybersecurity" as the research area.

The platform queries the database and returns a list of open thesis proposals that match these filters. The student can then open individual thesis entries, read the descriptions, check the supervising chair, and access the original source website if they want to apply or contact the responsible advisor.

This scenario is useful when the student already has a clear idea of the field they are interested in and wants to narrow down the available options using explicit criteria.

#### Scenario 2: Natural-Language Search

A student does not know the exact research area or keywords they should search for, but they know the kind of thesis they would like to work on. Instead of using filters, they write:

> I am interested in a practical thesis involving machine learning, preferably with real-world data and some implementation work.

The platform converts this natural-language description into an embedding vector and compares it with the stored thesis embeddings. It then returns thesis proposals that are semantically close to the student's interest, even if the exact words used by the student do not appear in the thesis title or description.

This scenario is useful when the student has a general interest but does not know the exact terminology used by different chairs.

#### Scenario 3: Discovering Overlooked Topics

A Bachelor's student is looking for a thesis but only knows a few well-known chairs. Without the platform, the student would likely check only those chairs' websites and might miss relevant topics published by smaller or less visible chairs.

Using the platform, the student can browse thesis proposals from many different chairs in one place. This increases the chance of discovering interesting topics that would otherwise have been overlooked.

This scenario demonstrates how the platform reduces the information gap between students and chairs.

#### Scenario 4: AI-Assisted Thesis Entry Creation

A scraping microservice visits a chair website and finds a page containing an open thesis proposal. However, the page structure is different from other chair websites, and the relevant information is embedded in normal text instead of being presented in a table or standardized format.

The system uses AI to extract the relevant fields, such as title, degree type, research area, advisor, chair, description, and application link. The extracted information is transformed into a structured JSON object and then stored in the database.

This scenario shows how AI helps the platform handle the lack of standardization across chair websites.

#### Scenario 5: Thesis Overview Generation

Some thesis postings contain very long and detailed descriptions, while others only provide a short paragraph. To make the browsing experience more consistent, the platform generates a short overview for each thesis.

For long descriptions, the overview summarizes the most important information. For shorter descriptions, it reformulates the available content into a clearer and more readable format without inventing unsupported details.

This helps students compare thesis topics more quickly and decide which ones they want to inspect in more detail.




# *First Product Backlog* 

|  ID | User Story                                                                                                                                                                               |
| --: | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
|   1 | As a **student**, I want to **browse all currently available open theses** in one platform, so that I do not have to manually visit many different chair websites.                       |
|   2 | As a **student**, I want to **filter thesis proposals by degree type, research area, chair, and keywords**, so that I can quickly find topics that match my requirements.                |
|   3 | As a **student**, I want to **open a thesis detail page**, so that I can read the full description, see the supervising chair, advisor information, and access the original source link. |
|   4 | As a **student**, I want to **describe my thesis interests in natural language**, so that I can find relevant topics even if I do not know the exact keywords or chair names.            |
|   5 | As a **student**, I want to **see AI-generated thesis overviews**, so that I can quickly understand and compare thesis topics without reading long descriptions first.                   |
|   6 | As a **student**, I want to **see why a thesis matches my natural-language query**, so that I can better understand the search results and decide whether the topic is relevant.         |
|   7 | As an **administrator**, I want to **manage the list of chair websites used for scraping**, so that new sources can be added and outdated sources can be removed.                        |