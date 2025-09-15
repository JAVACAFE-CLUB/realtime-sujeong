# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build and Test
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :collector:build
./gradlew :processor:build
./gradlew :indexer:build
./gradlew :serving:build

# Run tests for all modules
./gradlew test

# Run tests for specific module
./gradlew :collector:test
```

### Running Applications
```bash
# Start infrastructure services
docker-compose up -d

# Run specific module (from module directory)
cd collector && ./gradlew bootRun
cd processor && ./gradlew bootRun
cd indexer && ./gradlew bootRun
cd serving && ./gradlew bootRun
```

### Infrastructure Management
```bash
# Start Kafka, MinIO, and monitoring services
docker-compose up -d

# Check Kafka topics
docker exec kafka kafka-topics --bootstrap-server localhost:29092 --list

# Access services
# - Kafka UI: http://localhost:8081
# - MinIO Console: http://localhost:9001 (admin/admin123)
# - MinIO API: http://localhost:9000
```

## Architecture Overview

This is a **microservices-based real-time data processing platform** built with Spring Boot 3.5.4 and Java 24. The system processes data through a Kafka-based pipeline with the following modules:

### Module Structure
- **`common/`** - Shared entities and utilities (library module)
- **`collector/`** - Data collection service (RSS news, Wikimedia)  
- **`processor/`** - Data transformation and enrichment
- **`indexer/`** - Elasticsearch indexing service
- **`serving/`** - REST API and user management

### Data Flow Architecture
1. **Collection** → Raw data from RSS feeds and Wikimedia dumps
2. **Storage** → MinIO for large files, Kafka for metadata and small payloads  
3. **Processing** → Transform and enrich data via Kafka consumers/producers
4. **Indexing** → Store processed data in Elasticsearch for search
5. **Serving** → REST APIs backed by MySQL and Elasticsearch

### Key Technologies
- **Spring Boot 3.5.4** with Java 24
- **Apache Kafka** for message streaming
- **Elasticsearch 8.x** for search capabilities
- **MinIO** for S3-compatible object storage
- **MySQL** for relational data (serving module)
- **Docker Compose** for local development infrastructure

### Configuration Patterns
- YAML-based configuration (`application.yml`) in each module
- Environment-specific profiles supported
- Kafka topics auto-created via docker-compose init container
- Centralized dependency management in root build.gradle

### Key Components

**Domain-Driven Design Structure:**
- **RSS Collection Domain** (`domain/rss/`)
  - `RssParser` - XML/RSS feed parsing
  - `RssNewsCollector` - RSS collection orchestration  
  - `DuplicateChecker` - Duplicate detection and caching
  - `NewsContentExtractor` - Web content extraction
- **Wiki Collection Domain** (`domain/wiki/`)
  - `WikiXmlProcessor` - XML stream processing
  - `WikiDataCollector` - Wiki collection orchestration
  - `WikiTextCleaner` - Wiki markup cleaning
- **Infrastructure Layer** (`infrastructure/`)
  - `DataStorageService` - Unified MinIO storage operations
- **Service Layer** (`service/`)
  - `NewsCollectionService` - High-level collection orchestration
- **Common Models**
  - `CrawledData` - Common data model for collected information
  - `DataReference` - MinIO object reference metadata

### Development Notes
- Multi-module Gradle project with shared common library
- Uses Testcontainers for integration testing with Kafka/Elasticsearch
- All modules depend on the `common` module for shared entities
- Collection service runs on port 8083, other ports vary by module
- Korean language support configured (UTF-8 encoding)
- **Refactored Architecture** - Collector module follows Domain-Driven Design with clear separation of concerns
- **Reduced Logging** - Optimized from 65+ log statements to essential progress tracking only
- **Shared Infrastructure** - Common storage operations abstracted to prevent code duplication

### Collector Module Architecture
The collector module has been refactored from monolithic services to focused, single-responsibility classes:

**Before Refactoring Issues:**
- Monolithic services with multiple responsibilities (554+ lines each)
- Mixed concerns: parsing, web crawling, storage, messaging in single classes
- Excessive logging (65+ statements) causing performance impact
- Duplicate storage logic across services

**After Refactoring Benefits:**
- Clear domain separation (`rss/` vs `wiki/`)
- Single responsibility principle compliance
- Shared infrastructure layer
- Optimized logging (5000-item intervals vs every item)
- Improved testability and maintainability

### Kafka Topics
- `wiki-data` - Wikimedia dump processing results
- `naver-rss-news` - RSS news feed data  
- `raw-data-topic` - Raw collected data
- `news-data-topic` - Processed news data
- `social-data-topic` - Social media data (future use)