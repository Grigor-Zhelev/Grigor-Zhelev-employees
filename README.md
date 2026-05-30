# Employee Project Overlap Finder

A **Java Spring Boot** REST API application that identifies which pairs of employees have worked together on the same projects for the longest period of time. Built to demonstrate backend API design, data modeling, and algorithmic problem-solving.

## Overview

Given a CSV dataset of employee project assignments, the application finds the pair of employees who have collaborated on shared projects for the maximum combined duration. Handles open-ended assignments (NULL end dates treated as today) and overlapping date ranges across multiple projects.

## Features

- CSV file upload via web UI
- Automatic detection of employee pairs with maximum shared project time
- Handles `NULL` end dates (treated as current date)
- Supports multiple date formats
- REST API with JSON responses
- Simple HTML frontend for file upload and results display

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Java / Spring Boot |
| Build Tool | Maven |
| API | RESTful API |
| Frontend | HTML / JavaScript |
| Data Input | CSV file upload |

## Algorithm

For each pair of employees who worked on the same project, the overlap period is calculated as:

```
overlap = min(DateTo1, DateTo2) - max(DateFrom1, DateFrom2)
```

The pair with the highest total overlap across all shared projects is returned as the result.

## CSV Format

```csv
EmpID, ProjectID, DateFrom, DateTo
143, 12, 2013-11-01, 2014-01-05
218, 12, 2013-12-01, 2014-02-01
143, 10, 2009-01-01, 2011-04-27
218, 10, 2010-01-01, NULL
145, 10, 2010-03-01, 2010-05-01
300, 20, 2020-01-01, 2020-01-10
301, 20, 2020-01-05, 2020-01-15
```

- `EmpID` — Employee identifier
- `ProjectID` — Project identifier
- `DateFrom` — Start date of assignment
- `DateTo` — End date of assignment (`NULL` = still active, treated as today)

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Run the application

```bash
mvn spring-boot:run
```

Open your browser at: `http://localhost:8080`

### API Endpoint

```
POST /upload
Content-Type: multipart/form-data

Response:
{
  "employee1": 143,
  "employee2": 218,
  "daysWorkedTogether": 127
}
```

## Project Structure

```
src/
└── main/
    ├── java/
    │   └── com/.../employees/
    │       ├── controller/     ← REST controller
    │       ├── model/          ← Employee, ProjectAssignment
    │       └── service/        ← Overlap calculation logic
    └── resources/
        └── static/             ← HTML frontend
```

## Author

**Grigor Zhelev** — Senior Software Engineer  
[LinkedIn](https://www.linkedin.com/in/grigor-zhelev-ph-d-6312946/) | [GitHub](https://github.com/Grigor-Zhelev)
