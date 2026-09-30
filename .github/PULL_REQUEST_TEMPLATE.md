name: Pull request
description: Propose changes for review before merging
labels: pull-request
body:
  - type: input
    id: title
    attributes:
      label: Summary of changes
    validations:
      required: true

  - type: dropdown
    id: type
    attributes:
      label: Type of change
      options:
        - Feature (new MVP behaviour)
        - Bug fix
        - Documentation
        - Infrastructure / CI-CD
    validations:
      required: true

  - type: checkboxes
    id: mvp-area
    attributes:
      label: MVP area affected
      options:
        - label: Data/event entry
        - label: Searchable dashboard
        - label: Summary indicators
        - label: Status drill-down
        - label: Alert/exception view
        - label: Not applicable

  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: mvn clean package passes
        - label: mvn test passes (7/7)
        - label: Verified on http://localhost:8081/food-distribution-tracker/
        - label: No unrelated changes in this diff

  - type: textarea
    id: related
    attributes:
      label: Related issues
      placeholder: Closes #1
